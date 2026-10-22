# AniHoyo — Triển khai bằng Podman + Nginx

Stack đầy đủ: **Nginx (web server + reverse proxy) → Spring Boot API → MySQL / Redis / SeaweedFS (ảnh) / Cloudflare R2 (video)**.

> **Chia vai trò lưu trữ**
> * **Ảnh** (poster/`visual`/`slider`, `thumbnail`, `avatar`) → **SeaweedFS**, phục vụ qua backend tại `/storage/**` như cũ (URL, cột DB và frontend không đổi).
> * **Video** (file gốc + HLS) → **Cloudflare R2**, phát qua CDN `media.108pog.site`.
>
> Trước đây ảnh nằm trên đĩa của chính container backend (`./data/uploads`), không scale ngang được.
> Nay ảnh nằm trong object store, nhiều instance backend dùng chung.

---

## 1. Kết quả kiểm tra backend & R2

### 1.1. Code backend — **đã sẵn sàng** ✅

Toàn bộ luồng R2 đã được implement:

| Thành phần | File | Vai trò |
|---|---|---|
| Dependency | `pom.xml` | `software.amazon.awssdk:s3:2.31.61` |
| Cấu hình | `config/R2StorageProperties.java` | Bind `storage.r2.*` |
| S3 client | `config/R2StorageConfiguration.java` | Region `auto`, path-style, endpoint `https://<account>.r2.cloudflarestorage.com` |
| Nghiệp vụ | `services/implement/R2MediaStorageService.java` | `storeSourceVideo`, `downloadSourceVideo`, `uploadHlsDirectory`, `mediaResource` (stream HLS ra API), `masterPlaylistUri`, `objectUri` |
| Upload gốc | `controller/EpisodeController.java:99` | `POST /api/v1/upload/video` → bucket **private** `anihoyo-source` |
| Transcode | `services/implement/EpisodeService.java` (`processVideo`) | Chạy **nền**: tải gốc về → FFmpeg HLS → upload lên bucket `anihoyo-media`; trạng thái lưu ở `episodes.hls_status` |
| Phát video | `controller/StreamController.java` | `R2_HLS_RELAY=true` (mặc định): stream playlist/segment từ R2 qua API (cùng origin). `false`: trả `302` sang CDN |

### 1.1b. Lưu ảnh bằng SeaweedFS — **đã implement & kiểm chứng** ✅

Ảnh không còn ghi ra đĩa container. Luồng mới:

| Thành phần | File | Vai trò |
|---|---|---|
| Hạ tầng | `docker-compose.yml` → service `seaweedfs` | 1 tiến trình `weed server` chạy master(9333) + volume(8080) + filer(8888) + S3 gateway(8333) |
| Cấu hình | `config/SeaweedFSStorageProperties.java` | Bind `storage.seaweedfs.*` |
| S3 client | `config/SeaweedFSStorageConfiguration.java` | Bean `seaweedfsS3Client`, path-style, endpoint `http://seaweedfs:8333` |
| Nghiệp vụ | `services/implement/SeaweedFSMediaStorageService.java` | `put`, `head`, `open`, tự tạo bucket lúc khởi động |
| Upload | `services/implement/FileServiceImpl.java` | `storeFile` → `seaweedfs.put(file, "<folder>/<file>")` |
| Phục vụ | `controller/MediaController.java` | `GET /storage/**` stream object từ SeaweedFS |
| Dự phòng | `MediaController.localFallback()` | Ảnh chưa migrate vẫn đọc được từ `/uploads` cũ |
| Nginx | `deploy/nginx/conf.d/anihoyo.conf` → `location /media/` | Đọc thẳng bucket qua filer (bỏ qua backend) |
| Migrate | `deploy/seaweedfs/migrate-uploads.ps1` | Đẩy ảnh cũ trong `data/uploads` lên bucket |

**Vì sao URL không đổi:** khoá object chính là `<folder>/<file>` — đúng bằng phần đuôi của
`/storage/<folder>/<file>`. `storeFile()` vẫn trả về **mỗi tên file** như trước, nên cột
`thumb` / `thumbnail` / `slider` / `avatar` trong DB và mọi tham chiếu trong frontend giữ nguyên.

**Vì sao nginx đọc qua filer mà không qua S3 gateway:** ngay khi có credential, SeaweedFS S3
tắt chế độ "allow all" và đòi chữ ký SigV4 trên **mọi** request (đã kiểm chứng: `GET /` trả `403`).
Trình duyệt không ký được, nên đường đọc công khai đi qua filer HTTP (`/buckets/<bucket>/<key>`),
nơi cho phép đọc ẩn danh. Backend thì nói chuyện với S3 gateway bằng credential trong `.env`.

Endpoint `POST /api/v1/add-episode` **không còn** gọi `processVideo()` đồng bộ: nó lưu episode
(`hlsStatus=PENDING`) rồi đẩy việc chuyển mã sang thread nền, nên request trả về ngay. Trạng thái
(`PENDING` → `PROCESSING` → `READY`/`FAILED` + `hlsError`) nằm trong `EpisodeDTO`; xem mục 7 để
biết cách xử lý lại một tập.

### 1.2. Credential & bucket thật — **đã kiểm chứng** ✅

Đã test trực tiếp bằng SigV4 (`curl --aws-sigv4`) với key trong `.env`:

```
HEAD  /anihoyo-source                     -> 200   (bucket tồn tại, có quyền)
HEAD  /anihoyo-media                      -> 200   (bucket tồn tại, có quyền)
PUT   /anihoyo-media/hls/_smoketest/...   -> 200   (upload được)
GET   /anihoyo-media/hls/_smoketest/...   -> 200   (đọc lại đúng nội dung)
DELETE /anihoyo-media/hls/_smoketest/...  -> 204   (đã dọn dẹp)
GET   https://media.108pog.site/<key>     -> 200   (custom domain trỏ đúng bucket media)
```

> Kết luận: **credential hợp lệ, cả 2 bucket dùng được, `/anihoyo-source` đang trống (chưa upload phim nào), `/anihoyo-media` đang trống, CDN `media.108pog.site` hoạt động.**
> Lưu ý: token R2 này bị giới hạn theo bucket nên `ListBuckets` trả `403` — đây là điều bình thường, không phải lỗi.

### 1.3. Một lỗi đã phát hiện và sửa ⚠️ → ✅

`AniHoyo-backend-server/Dockerfile` dùng base image **`openjdk:17-slim`** — tag này **không còn tồn tại** trên Docker Hub (repo `openjdk` đã bị deprecate), nên `docker build`/`podman build` **luôn thất bại**:

```
Error: reading manifest 17-slim in docker.io/library/openjdk: manifest unknown
```

Đã thay bằng:

- Build stage: `maven:3.9-eclipse-temurin-17`
- Runtime stage: `eclipse-temurin:17-jre-jammy` (vẫn cài `ffmpeg` + `ffprobe` cho HLS)

### 1.4. Lỗi frontend đã sửa ⚠️ → ✅

Hai file **hardcode** địa chỉ WebSocket, không dùng `VITE_BACKEND_URL`:

- `src/components/client/feature/comment.list.jsx:27`
- `src/components/client/feature/notification.jsx:21`

```js
const socket = new SockJS("http://localhost:8083/ws");   // ❌ luôn trỏ cổng 8083
```

Khi chạy sau nginx, trình duyệt sẽ cố kết nối thẳng `localhost:8083`
(không được publish, và sai origin) → **bình luận realtime + thông báo hỏng**.
Đã sửa thành:

```js
const socket = new SockJS(`${import.meta.env.VITE_BACKEND_URL ?? "http://localhost:8083"}/ws`);
```

Với `VITE_BACKEND_URL=""` → URL là `/ws` (cùng origin) → nginx nâng cấp WebSocket và proxy tới backend.
Ở môi trường dev (`VITE_BACKEND_URL=http://localhost:8083`) hành vi cũ được giữ nguyên.

---

## 2. Kiểm chứng thực tế (đã chạy)

Stack đã được build và chạy thật bằng Podman, toàn bộ **5 container** đều `healthy`:

```
anihoyo-mysql     | Up (healthy) | 3306/tcp
anihoyo-redis     | Up (healthy) | 6379/tcp
anihoyo-seaweedfs | Up (healthy) | 8333, 8888/tcp (+ 127.0.0.1:8333, 127.0.0.1:9333)
anihoyo-backend   | Up (healthy) | 8083/tcp
anihoyo-nginx-proxy       | Up (healthy) | 0.0.0.0:80->80/tcp
```

### 2.1. Luồng ảnh → SeaweedFS (lần chạy mới nhất)

| Hạng mục | Lệnh / URL | Kết quả |
|---|---|---|
| Backend tạo bucket | log backend | `SeaweedFS bucket 'anihoyo-media' created.` (lần sau: `is ready.`) |
| Upload ảnh | `POST /api/v1/files` (`folder=visual`) | `200` → `1759...-final.png` |
| **Không ghi ra đĩa** | `Get-ChildItem data\uploads -Recurse -File` | **0 file** (trước đây ảnh nằm ở đây) |
| Object nằm trong bucket | `aws s3 ls s3://anihoyo-media/visual/` | `70  1790591791507-final.png` |
| Phục vụ qua backend | `GET /storage/visual/<file>` | `200 image/png`, **70B** |
| Phục vụ trực tiếp qua nginx | `GET /media/visual/<file>` | `200 image/png`, **70B** |
| Tính toàn vẹn | so sánh byte với file gốc | `identical=True` cho **cả hai** đường |
| Object không tồn tại | `GET /storage/visual/nope.png` | `404` |
| Tên file chứa `..` | `GET /storage/visual/a..png` | `404` (không bị chặn nhầm) |
| Path traversal | `GET /storage/../../../etc/passwd` | `400`, **không** trả nội dung `/etc/passwd` |
| S3 gateway ẩn danh | `GET http://127.0.0.1:8333/` | `403 AccessDenied` (đã bật xác thực) |
| Migrate ảnh cũ | `migrate-uploads.ps1 -DryRun` rồi chạy thật | `visual/legacy-avatar.png` lên bucket; sau khi **xoá file local** vẫn trả `200` → chứng minh đọc từ object store |
| Không hồi quy | `/` · `/healthz` · `/api/v1/test-cors` | `200` · `200` · `401` |

### 2.2. Luồng video → R2 (kiểm chứng ở lần chạy trước)

| Hạng mục | Lệnh / URL | Kết quả |
|---|---|---|
| Nginx phục vụ SPA | `GET http://localhost/` | `200` (index.html) |
| SPA deep-link fallback | `GET http://localhost/some/deep/route` | `200` |
| Healthcheck nginx | `GET /healthz` | `200 ok` |
| Nginx → backend | `GET /api/v1/test-cors` | `401` (đã tới Spring Security) |
| WebSocket qua proxy | `GET /ws/info` | `200` `{"websocket":true,...}` |
| Đăng nhập | `POST /api/v1/auth/login` | `200`, role `ADMIN`, có `access_token` |
| MySQL | log backend | `Database version: 8.4.11`, Hibernate tạo schema OK |
| Redis | log backend | `Redis connected !` |
| R2 khởi tạo | log backend | Không có lỗi → bean `r2S3Client` tạo thành công |
| **Upload phim lên R2** | `POST /api/v1/upload/video` | **`200`** → key `raw/<uuid>-smoketest.mp4` |
| **Xác minh object trên R2** | SigV4 `HEAD` + `GET` | **`200`**, đọc lại đúng nội dung |
| Dọn dẹp | SigV4 `DELETE` | `204`, sau đó `404` |
| Log lỗi nginx | `podman logs anihoyo-web` | Không có `error`/`emerg` |

> **Kết luận: ảnh giờ nằm trên SeaweedFS (không còn ghi ra đĩa container), phục vụ đúng qua
> `/storage/**` lẫn `/media/**`; luồng video trên R2 vẫn nguyên vẹn.**
> Mọi object test đã được xoá; cả 3 bucket (2 R2 + 1 SeaweedFS) hiện trống.

> ⚠️ **Phạm vi kiểm chứng:** CSDL của môi trường này đang trống (`0` episodes / `0` seasons /
> `0` films), nên luồng video → R2 **không thể chạy lại end-to-end** ở lần này; phần R2 giữ
> nguyên mã nguồn, chỉ thêm `@Qualifier` để tách bean khỏi SeaweedFS. Script migrate cũng
> được kiểm chứng bằng file tổng hợp vì `data/uploads` đang rỗng.


---

## 3. Kiến trúc

```
                    ┌──────────────────────────────────────────┐
   Browser  ──────► │  anihoyo-web  (Nginx :80 → host :80)     │
                    │  • /            → React SPA (dist)       │
                    │  • /api/        → backend:8083           │
                    │  • /storage/    → backend:8083 (ảnh)     │
                    │  • /media/      → seaweedfs:8888 (filer) │
                    │  • /ws          → backend:8083 (STOMP)   │
                    └───────────────────┬──────────────────────┘
                                        │ anihoyo-net
                    ┌───────────────────▼──────────────────────┐
                    │  anihoyo-backend (Spring Boot :8083)     │
                    │  profile: prod                           │
                    │  • /storage/**  ← S3 SeaweedFS           │
                    │  • /api/v1/upload/video → R2             │
                    └───┬───────────┬───────────┬──────────────┘
                        │           │           │
        ┌───────────────▼──┐ ┌──────▼───────┐ ┌─▼────────────────────┐
        │ anihoyo-mysql    │ │ anihoyo-     │ │ anihoyo-seaweedfs    │
        │  MySQL 8.4       │ │ redis  Redis7│ │  weed server 4.46    │
        │  volume          │ │  volume      │ │  master+volume+      │
        └──────────────────┘ └──────────────┘ │  filer+S3 :8333      │
                                              │  volume seaweedfs-data│
                                              └──────────────────────┘
                        │
        ┌───────────────▼──────────────────────────────────────────┐
        │  Cloudflare R2 (video, không đổi)                        │
        │  anihoyo-source (private) · anihoyo-media (public CDN)   │
        └──────────────────────────────────────────────────────────┘
```

### Ảnh đi đường nào?

| Đường | Luồng | Dùng khi |
|---|---|---|
| `/storage/<folder>/<file>` | Browser → nginx → **backend** → SeaweedFS S3 → stream về | Mặc định; URL không đổi nên frontend/DB không phải sửa |
| `/media/<folder>/<file>` | Browser → nginx → **SeaweedFS filer** (bỏ qua backend) | Đọc trực tiếp object lớn; hiện dùng để kiểm tra/tương lai chuyển HLS |

Cả hai đều trả đúng nội dung như nhau (đã kiểm chứng byte-for-byte).


### Vì sao SPA gọi API qua cùng origin?

`CorsConfigure.java` chỉ cho phép `localhost:3000/4173/5173`. Nếu build frontend với
`VITE_BACKEND_URL=http://localhost:8083` thì trình duyệt ở `http://localhost`
sẽ bị **chặn CORS**. Vì vậy image web build SPA với `VITE_BACKEND_URL=""` → mọi request
thành đường dẫn tương đối (`/api/v1/...`, `/storage/...`) và được nginx proxy. Không cần
sửa CORS, chạy được trên mọi domain/IP.

---

## 4. Các file đã tạo

```
D:\anihoyo\
├── docker-compose.yml                  # ★ file compose chính (5 service)
├── .env                                # cấu hình + secret thật (đang dùng được ngay)
├── .env.example                        # bản mẫu để copy
├── .dockerignore                       # giảm build context cho image web
├── data\uploads\                       # bind-mount → /uploads (dự phòng ảnh cũ + video khi R2 tắt)
├── deploy\
│   ├── nginx\
│   │   ├── Dockerfile                  # node build SPA → nginx:1.27-alpine
│   │   ├── nginx.conf                  # cấu hình http (gzip, timeout, upload 1GB)
│   │   ├── conf.d\anihoyo.conf         # ★ virtual host: thêm location /media/ → SeaweedFS
│   │   └── snippets\proxy_headers.conf # header proxy dùng chung (Host, X-Forwarded-*)
│   ├── seaweedfs\
│   │   └── migrate-uploads.ps1         # ★ đẩy ảnh cũ trong data/uploads lên bucket
│   └── mysql\
│       ├── initdb\01-init.sql          # ép charset utf8mb4 cho database
│       └── migrations\                 # ★ migration cho DB đã tồn tại (Hibernate không sửa cột cũ)
│           ├── 002-widen-media-url-columns.sql
│           └── 003-sync-permission-paths.sql
├── AniHoyo-backend-server\
│   ├── Dockerfile                      # ★ đã sửa base image
│   └── .dockerignore
└── AniHoyo-frontend-src\
    └── src\components\client\feature\
        ├── comment.list.jsx            # ★ đã sửa URL SockJS
        └── notification.jsx            # ★ đã sửa URL SockJS
```

### File backend thêm/sửa cho SeaweedFS

```
AniHoyo-backend-server\src\main\java\com\HieuPahm\AniHoyo\
├── config\
│   ├── SeaweedFSStorageProperties.java        # ★ MỚI  bind storage.seaweedfs.*
│   ├── SeaweedFSStorageConfiguration.java     # ★ MỚI  bean seaweedfsS3Client
│   ├── R2StorageConfiguration.java            # ★ SỬA  đặt tên bean r2S3Client
│   └── StaticResourceWebConfig.java           # ★ SỬA  tắt khi SeaweedFS bật
├── controller\
│   └── MediaController.java                   # ★ MỚI  GET /storage/** từ SeaweedFS
└── services\implement\
    ├── SeaweedFSMediaStorageService.java      # ★ MỚI  put/head/open + tạo bucket
    ├── FileServiceImpl.java                   # ★ SỬA  upload ảnh → SeaweedFS
    └── R2MediaStorageService.java             # ★ SỬA  nhận bean qua @Qualifier
```

> Hai `S3Client` cùng tồn tại (R2 + SeaweedFS) nên cả hai bean đều được đặt tên và
> inject qua `@Qualifier`; nếu không Spring sẽ báo lỗi ambiguous khi khởi động.


---

## 5. Chạy stack

```powershell
cd D:\anihoyo

# Build + khởi chạy toàn bộ
podman compose -f docker-compose.yml up -d --build

# Xem trạng thái
podman compose -f docker-compose.yml ps

# Log
podman compose -f docker-compose.yml logs -f backend
podman compose -f docker-compose.yml logs -f seaweedfs
podman compose -f docker-compose.yml logs -f web

# Dừng (giữ dữ liệu)
podman compose -f docker-compose.yml down

# Dừng và xoá luôn volume MySQL/Redis/SeaweedFS
podman compose -f docker-compose.yml down -v
```

> `podman compose` sẽ gọi `podman-compose` (đã có sẵn: podman 6.0.2 + podman-compose 1.6.0).
> Có thể dùng trực tiếp `podman-compose -f docker-compose.yml up -d` nếu muốn.

### ⚠️ Bốn điểm cần lưu ý khi build / chạy

**1. `podman compose build` không build được image `web`.** `podman-compose` không đọc key
`dockerfile:` khi context là `.` và báo:

```
Error: no Containerfile or Dockerfile specified or found in context directory, D:\anihoyo
```

Image `backend` thì build bình thường. Cách chắc chắn nhất là build trực tiếp rồi `up`:

```powershell
podman build -f AniHoyo-backend-server/Dockerfile -t localhost/anihoyo-backend:latest AniHoyo-backend-server
podman build -f deploy/nginx/Dockerfile           -t localhost/anihoyo-web:latest     .
podman compose -f docker-compose.yml up -d
```

**2. `up -d` không tự tạo lại container khi image đã đổi.** Sau khi build lại, container cũ
vẫn chạy (kiểm tra `podman inspect anihoyo-backend --format '{{.Image}}'` so với
`podman images`). Phải ép:

```powershell
podman compose -f docker-compose.yml up -d --force-recreate backend
```

**3. `data\uploads` phải tồn tại.** Đây là bind-mount của backend; nếu thư mục bị xoá,
container không start được (`crun: cannot stat /mnt/d/anihoyo/data/uploads`). Repo giữ sẵn
`data/uploads/.gitkeep`.

**4. Cổng 80 cần nới giới hạn trong máy ảo podman (chỉ làm một lần cho mỗi máy).**
Podman ở đây chạy **rootless**, mặc định `net.ipv4.ip_unprivileged_port_start=1024`, nên
`rootlessport` từ chối bind cổng 80:

```
rootlessport cannot expose privileged port 80 ... (currently 1024)
```

Đã xử lý sẵn trên máy này; nếu dựng lại máy ảo (hoặc máy mới) thì chạy lại:

```powershell
podman machine ssh "printf 'net.ipv4.ip_unprivileged_port_start=80\n' | sudo tee /etc/sysctl.d/99-anihoyo-privileged-ports.conf > /dev/null; grep -q ip_unprivileged_port_start /etc/sysctl.conf || printf '\nnet.ipv4.ip_unprivileged_port_start=80\n' | sudo tee -a /etc/sysctl.conf > /dev/null; sudo sysctl -w net.ipv4.ip_unprivileged_port_start=80"
```

Thiết lập này **nằm trong máy ảo, không nằm trong repo**, và sống sót qua reboot
(`systemd-sysctl` là unit `static`, luôn chạy lúc boot; `/etc/sysctl.conf` được áp dụng cuối
cùng nên có ưu tiên cao nhất).

> **Không muốn đụng vào máy ảo?** Chỉ cần đặt `WEB_PORT` trong `.env` thành cổng ≥ 1024
> (ví dụ `8080`) rồi `podman compose up -d --force-recreate web`.

### Vì sao `80:80` mà tài liệu cũ ghi 8080?

`ports: - "${WEB_PORT:-80}:80"` là mapping `host:container`. Số **80** đứng sau là cổng nginx
lắng nghe **bên trong container** (`listen 80`) — đây là quy ước cố định của image nginx.
Số **đầu** mới là cổng trên host, điều khiển bằng `WEB_PORT`. Đặt `WEB_PORT=80` thì site ở
`http://localhost` không cần hậu tố cổng.

Đổi cổng chỉ cần sửa `.env` rồi `--force-recreate web`, **không phải build lại image web**.

### ⚠️ Cổng 80 hiện CHỈ truy cập được từ chính máy này (loopback)

Podman machine trên Windows forward cổng published về **loopback** của host, không phải ra mọi
interface. Kiểm chứng trên máy này:

```
netstat -ano | findstr :80
  TCP    127.0.0.1:80     ...  LISTENING      <- chỉ có 2 dòng này
  TCP    [::1]:80         ...  LISTENING
  (không hề có 0.0.0.0:80 hay 192.168.1.x:80)

curl http://192.168.1.93/   ->  000 (không kết nối được)
```

Hệ quả:

| Truy cập từ | Kết quả |
|---|---|
| `http://localhost/` (chính máy này) | ✅ `200` |
| `http://192.168.1.93/` (máy khác trong LAN) | ❌ không vào được |
| Từ Internet | ❌ không vào được |

Nếu chỉ cần dùng nội bộ / dev thì **không phải làm gì thêm**. Muốn mở ra LAN hoặc Internet thì
phải xử lý thêm ở tầng podman machine (ví dụ tắt user-mode networking để WSL dùng networking
gốc, hoặc dựng reverse proxy/tunnel phía trước) — đây là việc ở tầng hạ tầng, chưa làm trong
lần này. Với HTTPS công khai thì thường đi đường Cloudflare Tunnel hoặc reverse proxy ngoài
thay vì publish thẳng cổng 443 từ máy này.



### Migration ảnh cũ sang SeaweedFS

Chỉ cần chạy **một lần**, sau khi stack đã lên:

```powershell
cd D:\anihoyo
.\deploy\seaweedfs\migrate-uploads.ps1 -DryRun   # xem trước
.\deploy\seaweedfs\migrate-uploads.ps1           # chạy thật
```

Script chỉ đẩy các thư mục ảnh (`visual`, `thumbnail`, `avatar`, `slider`, `temp`) và **không xoá**
gì trên đĩa. Thêm `-All` nếu muốn đẩy toàn bộ `data/uploads`.

Trong lúc chưa migrate xong, đặt `SEAWEEDFS_FALLBACK_TO_LOCAL=true` (mặc định) để backend đọc
ảnh chưa có trên bucket từ đĩa cũ thay vì trả `404`. Khi đã migrate xong và kiểm tra ổn:

```powershell
# sửa .env:  SEAWEEDFS_FALLBACK_TO_LOCAL=false
podman compose -f docker-compose.yml up -d --force-recreate backend
```


### Truy cập

| Dịch vụ | URL |
|---|---|
| Giao diện web | http://localhost |
| API | http://localhost/api/v1/... |
| Swagger UI | http://localhost/swagger-ui/index.html |
| Healthcheck nginx | http://localhost/healthz |
| Ảnh qua backend | http://localhost/storage/visual/&lt;file&gt; |
| Ảnh trực tiếp từ SeaweedFS | http://localhost/media/visual/&lt;file&gt; |
| SeaweedFS Master UI (chỉ loopback) | http://localhost:9333 |
| SeaweedFS S3 API (chỉ loopback) | http://localhost:8333 |
| MySQL | nội bộ `mysql:3306` (không publish — bật bằng cách bỏ comment `ports` trong compose) |

Tài khoản admin tạo tự động ở lần khởi động đầu: **admin@gmail.com / 123456**.

---

## 6. Kiểm tra sau khi chạy

```powershell
# 1) Tất cả container healthy
podman ps --filter 'name=anihoyo' --format '{{.Names}}\t{{.Status}}'

# 2) Nginx phục vụ SPA
curl.exe -s -o NUL -w "web   %{http_code}\n" http://localhost/

# 3) Nginx proxy tới backend (401 = Spring Security đang chạy, đã tới được backend)
curl.exe -s -o NUL -w "api   %{http_code}\n" http://localhost/api/v1/test-cors

# 4) Backend nhận đúng cấu hình R2 (không có exception "R2 storage is not configured")
podman logs anihoyo-backend 2>&1 | Select-String -Pattern 'R2|Started AniHoyoApplication|Tomcat started'

# 5) SeaweedFS đã sẵn sàng và backend đã tạo bucket
podman logs anihoyo-backend 2>&1 | Select-String -Pattern 'SeaweedFS'
# -> SeaweedFS bucket 'anihoyo-media' created.   (hoặc: is ready.)

# 6) Đăng nhập lấy token
curl.exe -s -X POST http://localhost/api/v1/auth/login -H "Content-Type: application/json" -d '{\"username\":\"admin@gmail.com\",\"password\":\"123456\"}'
```

### Upload ảnh lên SeaweedFS (luồng thật)

```powershell
# B1. Lấy access token
curl.exe -s -o "$env:TEMP\login.json" -X POST http://localhost/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"admin@gmail.com\",\"password\":\"123456\"}'
$token = [regex]::Match((Get-Content "$env:TEMP\login.json" -Raw), '"access_token"\s*:\s*"([^"]+)"').Groups[1].Value

# B2. Upload ảnh -> trả về TÊN FILE (không phải URL); folder do client chọn
curl.exe -s -X POST http://localhost/api/v1/files `
  -H "Authorization: Bearer $token" `
  -F "file=@D:\anh\poster.png" -F "folder=visual"
# -> {"fileName":"1759...-poster.png", ...}

# B3. Đọc lại qua backend (URL y như trước khi có SeaweedFS)
curl.exe -s -o NUL -w "storage %{http_code} %{content_type}\n" `
  "http://localhost/storage/visual/1759...-poster.png"

# B4. Hoặc đọc trực tiếp từ SeaweedFS qua nginx
curl.exe -s -o NUL -w "media   %{http_code} %{content_type}\n" `
  "http://localhost/media/visual/1759...-poster.png"

# B5. Xác minh object thật sự nằm trong bucket (không nằm trên đĩa)
podman run --rm --network anihoyo-net `
  -e AWS_ACCESS_KEY_ID=anihoyo `
  -e AWS_SECRET_ACCESS_KEY=$((Get-Content .env | Select-String '^SEAWEEDFS_SECRET_ACCESS_KEY=').Line -replace '^[^=]+=','') `
  -e AWS_DEFAULT_REGION=us-east-1 -e AWS_EC2_METADATA_DISABLED=true `
  docker.io/amazon/aws-cli:latest `
  s3 ls s3://anihoyo-media/visual/ --endpoint-url http://seaweedfs:8333
```

### Upload phim lên R2 (luồng thật)

```powershell
# B1. Lấy access token (trường tên là `access_token`, không phải accessToken)
curl.exe -s -o "$env:TEMP\login.json" -X POST http://localhost/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"admin@gmail.com\",\"password\":\"123456\"}'
$token = [regex]::Match((Get-Content "$env:TEMP\login.json" -Raw), '"access_token"\s*:\s*"([^"]+)"').Groups[1].Value

# B2. Upload file gốc -> trả về R2 key private (vd: raw/<uuid>-phim.mp4)
curl.exe -s -X POST http://localhost/api/v1/upload/video `
  -H "Authorization: Bearer $token" `
  -F "file=@D:\phim\tap01.mp4" -F "folder=videos"

# B3. Tạo episode với filePath = key ở B2
#     -> API lưu row rồi trả 201 NGAY, FFmpeg HLS chạy ở thread nền
#        (hlsStatus: PENDING -> PROCESSING -> READY/FAILED)
curl.exe -s -X POST http://localhost/api/v1/add-episode `
  -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
  -d '{\"title\":\"tap-01\",\"filePath\":\"raw/<uuid>-phim.mp4\",\"contentType\":\"video/mp4\",\"season\":{\"id\":1}}'

# B4. Xem trạng thái chuyển mã (hlsStatus + hlsError nằm trong từng episode)
curl.exe -s "http://localhost/api/v1/episodes/by-season/1" -H "Authorization: Bearer $token"

# B5. Chuyển mã lại nếu FAILED (hoặc với episode cũ chưa từng chạy pipeline)
curl.exe -s -X POST http://localhost/api/v1/process-episode/1 `
  -H "Authorization: Bearer $token"

# B6. Khi hlsStatus=READY -> master.m3u8 trả 200 (backend stream từ R2).
curl.exe -s -o NUL -w "master.m3u8 %{http_code} %{content_type}`n" http://localhost/api/v1/1/master.m3u8
# -> 200 application/vnd.apple.mpegurl
# Với R2_HLS_RELAY=false thì thay vào đó là:
# -> 302 Location: https://media.108pog.site/hls/episodes/1/<hash>/master.m3u8
```

---

## 7. Ghi chú & xử lý sự cố

**`client_max_body_size 1024m`** — backend cho phép upload 1000MB
(`spring.servlet.multipart.max-file-size`), nên nginx phải mở tương ứng, nếu không sẽ bị `413`.
Ngoài ra `/api/` bật `proxy_request_buffering off` để không đệm file 1GB xuống đĩa container.

**Timeout dài** — `proxy_read_timeout/proxy_send_timeout = 3600s`. Từ khi transcode chuyển sang
chạy nền, `POST /api/v1/add-episode` trả về trong vài chục ms, nên timeout này chỉ còn để phòng
trường hợp upload file lớn chậm.

**`ClassCastException: RestResponse cannot be cast to Resource`** (đã sửa) — Spring 6.2 gọi
`ResponseBodyAdvice#beforeBodyWrite` **cả khi body là null** (302 redirect, 404 rỗng, HEAD...),
trong khi converter đã được chọn theo *kiểu khai báo* của method. `FormatRestResponse` cũ bọc
null thành `RestResponse`, rồi `ResourceHttpMessageConverter` (method `ResponseEntity<Resource>`)
cast giá trị đó sang `Resource` → 500 cho `/api/v1/{id}/master.m3u8` và mọi ảnh `/storage/**`
không tồn tại. Cách sửa: chỉ bọc envelope khi converter được chọn là loại **generic** (Jackson);
các converter còn lại giữ nguyên body null để Spring tự ghi status/header.

**`R2_HLS_RELAY`** — mặc định `true`: backend stream HLS playlists/segments từ R2 nên request
**cùng origin** với SPA, không cần CORS trên bucket (hls.js chạy ở mọi trình duyệt). Đặt `false`
để trả `302` sang `R2_PUBLIC_BASE_URL` (CDN phục vụ video, nhẹ VPS hơn — nhưng **bắt buộc** bucket
phải có CORS policy cho origin của SPA, nếu không hls.js bị chặn và video không phát được):

```powershell
# Cần token Cloudflare có quyền "Workers R2 Storage: Edit" (token S3 hiện tại KHÔNG đủ):
#   Dashboard > R2 > anihoyo-media > Settings > CORS Policy
#   Allowed origins: *   |   Allowed methods: GET, HEAD   |   Allowed headers: *
curl.exe -s -D - -o NUL -H "Origin: http://localhost" `
  "https://media.108pog.site/hls/episodes/1/<hash>/master.m3u8" | Select-String access-control
# -> phải thấy: access-control-allow-origin: *
```

> ⚠️ **Ở chế độ `R2_HLS_RELAY=false`, Cloudflare cache cả 404** của bucket public trong ~3 giờ
> (`cache-control: max-age=10800`, `cf-cache-status: HIT`). Nếu playlist từng bị gọi *trước khi*
> chuyển mã xong (đã kiểm chứng: `master.m3u8` bị 404 cache, thêm `?cb=1` thì trả 200), hãy purge
> cache trong Dashboard > Caching > Purge Everything. Chế độ relay (mặc định) không dính vấn đề
> này vì backend luôn kiểm tra object trên R2 theo từng request.

**`R2_ENABLED=false` thì video gốc nằm ở volume `/uploads`, KHÔNG phải SeaweedFS** — FFmpeg
(`EpisodeService#processVideoLocally`) đọc `<base-uri>/videos/<filePath>`, nên
`POST /api/v1/upload/video` phải gọi `FileServiceImpl.storeLocalFile()` (ghi thẳng vào volume).
Trước đây nhánh này dùng nhầm `storeFile()` — hàm này đẩy vào **SeaweedFS** ngay khi
`SEAWEEDFS_ENABLED=true` (dùng cho ảnh) — nên upload "thành công" nhưng FFmpeg báo
`/uploads/videos/<name>.mp4: No such file or directory` và episode `FAILED`. Triệu chứng nhận biết:
video **không** xuất hiện trong bucket `anihoyo-source` (vì không đi qua R2) mà lại nằm ở
SeaweedFS key `videos/<name>`, đúng lúc `R2_ENABLED` bị đặt `false`.

**Chuyển mã HLS chạy nền** — `EpisodeService.processVideo()` chạy trên `hlsTaskExecutor`
(1 worker, xem `config/HlsProcessingConfig.java`). Row `episodes` giữ trạng thái
`hls_status`/`hls_error`; nếu container bị restart giữa chừng, `EpisodeService#resumeInterruptedProcessing`
(`@EventListener(ApplicationReadyEvent)`) tự đưa các tập `PENDING`/`PROCESSING` vào hàng đợi lại.
Trong admin có nút **Xử lý lại** (gọi `POST /api/v1/process-episode/{id}`) cho tập `FAILED`.

**RAM của podman machine** — máy ảo hiện được cấp `2GiB`
(`podman machine list`). JVM đã được giới hạn bằng `JAVA_MAX_RAM_PERCENT=60` trong `.env`.
Nếu transcode video lớn bị OOM, tăng RAM cho máy ảo rồi chạy lại:

```powershell
podman machine stop
podman machine set --memory 8192
podman machine start
```
*(lưu ý: thao tác này sẽ dừng mọi container đang chạy trên máy ảo)*

**TLS/cookie** — đang chạy HTTP nội bộ nên để `VIEW_COOKIE_SECURE=false`. Khi đưa lên
production có HTTPS, đặt `VIEW_COOKIE_SECURE=true`.

**Đổi mật khẩu DB / JWT** — sửa `.env` rồi `podman compose up -d --force-recreate backend`.
Nếu đổi `MYSQL_*` sau khi volume `mysql-data` đã tạo, phải `down -v` để MySQL khởi tạo lại.

**Ghi vào `/uploads`** — bind-mount `./data\uploads` → `/uploads`, tương ứng
`hieupham.upload-file.base-uri=file:///uploads/`. Từ khi có SeaweedFS, thư mục này **không còn
là nơi lưu ảnh mới**; nó chỉ dùng để:
* đọc dự phòng ảnh cũ chưa migrate (`SEAWEEDFS_FALLBACK_TO_LOCAL=true`),
* lưu ảnh local khi `SEAWEEDFS_ENABLED=false`,
* lưu video + HLS khi `R2_ENABLED=false`.

**R2 lỗi cấu hình thì app dừng ngay** — `application-prod.yml` đặt `R2_ENABLED=true` mặc định
và `R2StorageConfiguration` ném `IllegalStateException` nếu thiếu `R2_ACCOUNT_ID`/key. Đó là chủ ý
(fail-fast thay vì âm thầm ghi ra đĩa container).

**SeaweedFS lỗi cấu hình cũng dừng ngay** — `SEAWEEDFS_ENABLED=true` trong prod và
`SeaweedFSStorageConfiguration` ném `IllegalStateException` nếu thiếu endpoint/key/bucket.

**Profile `dev` cũng đi thẳng lên S3** — `application-dev.yml` bật sẵn
`storage.seaweedfs.enabled=true` (endpoint `http://localhost:8333`, vì compose publish gateway ra
loopback) và `storage.r2.enabled=true`, nên backend chạy trên Windows ghi ảnh vào SeaweedFS và
video/HLS lên R2 giống prod. Hệ quả: phải có `.env` (nạp qua `spring.config.import` trong
`application.yml`) và container `anihoyo-seaweedfs` phải đang chạy, nếu không app dừng ngay lúc
khởi động — tắt tạm bằng `$env:SEAWEEDFS_ENABLED='false'` / `$env:R2_ENABLED='false'`.

**Hai `S3Client` bean** — R2 và SeaweedFS đều dùng AWS SDK nên có 2 bean cùng kiểu `S3Client`.
Chúng được đặt tên `r2S3Client` / `seaweedfsS3Client` và inject bằng `@Qualifier`. Khi thêm
store thứ ba, nhớ làm tương tự, nếu không Spring sẽ báo `NoUniqueBeanDefinitionException`.

**Đổi bucket SeaweedFS** — nếu sửa `SEAWEEDFS_BUCKET` trong `.env`, phải sửa **cả**
`location /media/` trong `deploy/nginx/conf.d/anihoyo.conf` (đang hardcode `anihoyo-media`)
rồi build lại image web.

**Xác thực S3 của SeaweedFS** — cấu hình qua `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`
truyền cho service `seaweedfs`. Ngay khi có credential, S3 gateway **tắt chế độ cho phép ẩn danh**
(`GET /` trả `403`), nên đừng trỏ nginx `/media/` vào cổng `8333` — hãy dùng filer `8888` như hiện tại.
Đổi key thì phải đổi ở **cả hai** service (backend + seaweedfs) vì chúng dùng chung giá trị trong `.env`.

**Mở rộng SeaweedFS** — hiện là 1 container all-in-one (`weed server`) cho vừa 2GiB RAM. Khi cần
scale, tách thành các service `master` / `volume` / `filer` / `s3` riêng và thêm volume server;
backend chỉ nói chuyện qua S3 endpoint nên **không phải sửa code**, chỉ trỏ `SEAWEEDFS_ENDPOINT`
sang gateway mới (hoặc đặt load balancer trước các gateway).

**Dữ liệu SeaweedFS nằm ở volume `seaweedfs-data`** (`/data`: volume data + metadata filer leveldb2
+ metadata master). `podman compose down -v` sẽ **xoá sạch ảnh** — cân nhắc backup trước.

**Cột URL/ảnh phải đủ dài** — `seasons.trailer`, `seasons.thumb`, `episodes.file_path`,
`users.avatar` lưu URL tuyệt đối do R2/SeaweedFS trả về. Trước đây chúng là `VARCHAR(255)` nên
URL dài làm MySQL (strict mode) báo `Data truncation: Data too long for column 'trailer'`.
Hibernate `ddl-auto=update` **không sửa định nghĩa cột đã tồn tại**, nên với DB cũ phải chạy
migration một lần:

```powershell
podman exec -i anihoyo-mysql mysql -uroot -p"root_secret" anihoyo `
  < deploy\mysql\migrations\002-widen-media-url-columns.sql
```

File này cũng đổi các cột `nvarchar(n)` (MySQL hiểu là `utf8mb3`, mất ký tự 4 byte như emoji)
sang `VARCHAR(n) CHARACTER SET utf8mb4`. DB mới tạo bởi Hibernate đã đúng sẵn, không cần chạy.

**Đường dẫn trong bảng `permissions` phải khớp tuyệt đối với mapping** — ACL so sánh chuỗi
`apiPath` với `BEST_MATCHING_PATTERN_ATTRIBUTE` của request, nên một dòng sai/thiếu làm endpoint
**không role nào gọi được** (trong khi khách ẩn danh lại qua được, vì ACL chỉ chạy khi đã đăng nhập).
Hai lỗi đã sửa bằng migration:

```powershell
podman exec -i anihoyo-mysql mysql -uroot -p"root_secret" anihoyo `
  < deploy\mysql\migrations\003-sync-permission-paths.sql
```

* `DELETE /api/v1/delete-episode/{id}` từng bị lưu là `delete-epsiode` (lỗi chính tả) → xoá tập
  phim luôn 403.
* `GET /api/v1/seasons/related/{seasonId}` chưa có dòng nào → mục "phim liên quan" luôn 403 với
  người đã đăng nhập. Migration tạo dòng và cấp cho mọi role đang có quyền `GET /api/v1/seasons`.

**Danh sách path public nằm ở một chỗ** — `config/PublicPaths.java`, dùng chung cho
`SecurityConfiguration` (filter chain) và `InterceptorConfiguration` (ACL). Trước đây hai nơi giữ
hai bản riêng nên bị lệch: `POST /api/v1/{id}/view` và các playlist HLS là `permitAll` ở filter
chain nhưng vẫn bị ACL trả 403 cho người đã đăng nhập. Thêm endpoint public mới thì sửa
`PublicPaths`, không sửa hai nơi.

**Hợp đồng lỗi của API** — mọi lỗi trả về cùng một envelope
`{ statusCode, message, error, data, path, timestamp }`, trong đó `statusCode` **luôn bằng** HTTP
status thật (trước đây lỗi 403 bị trả về HTTP 400, lỗi SQL bị báo thành "không có quyền").
`path`/`timestamp` giúp đối chiếu log khi client chỉ thấy thông báo lỗi. Bảng mã lỗi chính:

| Tình huống | HTTP | `message` |
| --- | --- | --- |
| Sai JSON / sai enum / sai kiểu | 400 | `Nội dung JSON không hợp lệ hoặc sai kiểu dữ liệu` |
| Vi phạm `@Valid` trên body | 400 | `Dữ liệu gửi lên không hợp lệ` (kèm tên field) |
| Dữ liệu vượt độ dài cột (`Data too long`) | 400 | `...quá dài hơn giới hạn của cột '<cột>'...` |
| Vi phạm unique / khoá ngoại | 409 | `Dữ liệu đã tồn tại...` / `Dữ liệu tham chiếu không tồn tại...` |
| Sai tài khoản/mật khẩu | 401 | `Sai tài khoản hoặc mật khẩu` |
| Thiếu quyền theo bảng `permissions` | 403 | `Bạn không có quyền thực hiện thao tác này` |
| Không tìm thấy endpoint/tài nguyên | 404 | `Không tìm thấy tài nguyên được yêu cầu` |
| Sai method / Content-Type / file quá lớn | 405 / 415 / 413 | tương ứng |
| Lỗi hệ thống còn lại | 500 | tên exception + message gốc (kèm stack trace trong log) |

`AuthorityIntercepter` chỉ kiểm tra **request gốc** (`DispatcherType.REQUEST`) và chỉ với các
endpoint controller: trước đây nó chạy lại cho dispatch `/error`, nên mọi lỗi 500 bị thay bằng
"không có quyền" và giấu mất nguyên nhân thật.

