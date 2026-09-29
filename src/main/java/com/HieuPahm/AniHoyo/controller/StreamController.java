package com.HieuPahm.AniHoyo.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.HieuPahm.AniHoyo.domain.RestResponse;
import com.HieuPahm.AniHoyo.model.entities.Episode;
import com.HieuPahm.AniHoyo.repository.EpisodeRepository;
import com.HieuPahm.AniHoyo.services.implement.EpisodeService;
import com.HieuPahm.AniHoyo.services.implement.FileServiceImpl;
import com.HieuPahm.AniHoyo.services.implement.R2MediaStorageService;
import com.HieuPahm.AniHoyo.utils.constant.ChunkConstant;
import com.HieuPahm.AniHoyo.utils.constant.HlsStatus;

@RestController
@RequestMapping("/api/v1")
public class StreamController {

    @Value("${hieupham.upload-file.base-uri}")
    private String baseURI;

    private final EpisodeRepository episodeRepository;
    private final FileServiceImpl fileServiceImpl;
    private final R2MediaStorageService r2MediaStorageService;

    public StreamController(EpisodeService episodeService, EpisodeRepository episodeRepository,
            FileServiceImpl fileServiceImpl, R2MediaStorageService r2MediaStorageService) {

        this.episodeRepository = episodeRepository;
        this.fileServiceImpl = fileServiceImpl;
        this.r2MediaStorageService = r2MediaStorageService;
    }

    // Test endpoint to verify CORS
    @GetMapping("/test-cors")
    public ResponseEntity<String> testCors() {
        return ResponseEntity.ok("CORS is working!");
    }

    // Basic streaming ===> can not handle video resolutions
    @GetMapping("/stream/range/{id}")
    public ResponseEntity<?> streamVideoRange(
            @PathVariable("id") long id,
            @RequestHeader(value = "Range", required = false) String range) {

        if (r2MediaStorageService.isEnabled()) {
            // The original is intentionally private in R2. Clients must use HLS,
            // which is served directly by the CDN rather than relayed by this API.
            return ResponseEntity.status(HttpStatus.GONE)
                    .header(HttpHeaders.LINK, "</api/v1/" + id + "/master.m3u8>; rel=alternate")
                    .build();
        }

        Optional<Episode> episode = this.episodeRepository.findById(id);
        if (episode.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        URI uri = URI.create(baseURI + "videos/" + episode.get().getFilePath());
        Path path = Paths.get(uri);

        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String contentType = episode.get().getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        long fileLength;
        try {
            fileLength = Files.size(path);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        // nếu client không gửi range thì trả nguyên file
        if (range == null) {
            Resource resource = new FileSystemResource(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(fileLength)
                    .body(resource);
        }
        // --- xử lý Range header ---
        long rangeStart;
        long rangeEnd;

        String[] ranges = range.replace("bytes=", "").split("-");
        rangeStart = Long.parseLong(ranges[0]);

        if (ranges.length > 1 && !ranges[1].isEmpty()) {
            rangeEnd = Long.parseLong(ranges[1]);
        } else {
            rangeEnd = rangeStart + ChunkConstant.CHUNK_SIZE - 1;
        }
        if (rangeEnd >= fileLength) {
            rangeEnd = fileLength - 1;
        }
        // nếu file rỗng thì trả lỗi
        if (fileLength <= 0 || rangeStart > rangeEnd) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            inputStream.skip(rangeStart);
            long contentLength = rangeEnd - rangeStart + 1;
            byte[] data = new byte[(int) contentLength];
            int read = inputStream.read(data, 0, data.length);

            System.out.println("range start : " + rangeStart);
            System.out.println("range end   : " + rangeEnd);
            System.out.println("read bytes  : " + read);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
            headers.setContentLength(contentLength);
            headers.add("Accept-Ranges", "bytes");
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(data));

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // HLS - Adaptive streaming
    @GetMapping("/{id}/master.m3u8")
    public ResponseEntity<?> serverMasterFile(@PathVariable("id") long id) {
        Optional<Episode> episode = this.episodeRepository.findById(id);
        if (episode.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (r2MediaStorageService.isEnabled()) {
            HlsStatus status = episode.get().getHlsStatus();
            if (status != HlsStatus.READY) {
                // Redirecting to the CDN before the pipeline uploaded anything only
                // makes the player report an opaque 404. Answer with the real state
                // instead (409 + the same RestResponse envelope as every other error).
                RestResponse<Object> body = new RestResponse<>();
                body.setStatusCode(HttpStatus.CONFLICT.value());
                body.setMessage(playbackNotReadyMessage(id, status));
                body.setError(status == null ? "UNKNOWN" : status.name());
                body.setTimestamp(Instant.now());
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("episodeId", id);
                details.put("hlsStatus", status == null ? "UNKNOWN" : status.name());
                if (episode.get().getHlsError() != null) {
                    details.put("hlsError", episode.get().getHlsError());
                }
                body.setData(details);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
            }
            if (r2MediaStorageService.isRelayEnabled()) {
                return hlsObject(episode.get(), "master.m3u8");
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(r2MediaStorageService.masterPlaylistUri(episode.get()))
                    .build();
        }
        URI uri = URI.create(baseURI + "videos_hls/" + episode.get().getTitle() + "/master.m3u8");
        Path path = Paths.get(uri);

        System.out.println("Debug - Episode ID: " + id);
        System.out.println("Debug - Episode Title: " + episode.get().getTitle());
        System.out.println("Debug - Base URI: " + baseURI);

        if (!Files.exists(path)) {
            System.out.println("Debug - File not found at: " + path);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Resource resource = new FileSystemResource(path);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                .body(resource);
    }

    private static String playbackNotReadyMessage(long id, HlsStatus status) {
        if (status == HlsStatus.FAILED) {
            return "Xử lý HLS cho tập phim này thất bại, kiểm tra 'hlsError' của episode rồi xử lý lại.";
        }
        if (status == HlsStatus.PENDING || status == HlsStatus.PROCESSING) {
            return "Video đang được xử lý (chuyển mã HLS), vui lòng thử lại sau ít phút.";
        }
        return "Tập phim này chưa có HLS trên R2. Gọi POST /api/v1/process-episode/" + id + " để xử lý lại.";
    }

    /**
     * Streams one HLS object out of the media bucket so playback is same-origin
     * (no CORS rule needed on the bucket, works with hls.js in every browser).
     */
    private ResponseEntity<?> hlsObject(Episode episode, String relativePath) {
        Resource resource = r2MediaStorageService.mediaResource(episode, relativePath);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok()
                .contentType(hlsContentType(relativePath))
                .body(resource);
    }

    private static MediaType hlsContentType(String relativePath) {
        return relativePath.endsWith(".ts")
                ? MediaType.parseMediaType("video/mp2t")
                : MediaType.parseMediaType("application/vnd.apple.mpegurl");
    }

    // serve the segments
    @GetMapping("/{id}/{quality}/{segment}.ts")
    public ResponseEntity<?> serveSegments(
            @PathVariable("id") long id,
            @PathVariable String quality,
            @PathVariable String segment) {
        Optional<Episode> episode = this.episodeRepository.findById(id);
        if (episode.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Validate quality parameter (should end with 'p' like 360p, 720p, 1080p)
        if (!quality.endsWith("p") || !quality.matches("\\d+p")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (r2MediaStorageService.isEnabled()) {
            if (r2MediaStorageService.isRelayEnabled()) {
                return hlsObject(episode.get(), quality + "/" + segment + ".ts");
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(r2MediaStorageService.objectUri(episode.get(), quality + "/" + segment + ".ts"))
                    .build();
        }

        // create path for segment with quality folder
        URI uri = URI
                .create(baseURI + "videos_hls/" + episode.get().getTitle() + "/" + quality + "/" + segment + ".ts");
        Path path = Paths.get(uri);

        System.out.println("Debug - Segment path: " + path);

        if (!Files.exists(path)) {
            System.out.println("Debug - Segment file not found at: " + path);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Resource resource = new FileSystemResource(path);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, "video/mp2t")
                .body(resource);
    }

    // serve quality-specific playlist files (360p/index.m3u8, 720p/index.m3u8,
    // etc.)
    @GetMapping("/{id}/{quality}/index.m3u8")
    public ResponseEntity<?> serveQualityPlaylist(
            @PathVariable("id") long id,
            @PathVariable String quality) {
        Optional<Episode> episode = this.episodeRepository.findById(id);
        if (episode.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Validate quality parameter (should end with 'p' like 360p, 720p, 1080p)
        if (!quality.endsWith("p") || !quality.matches("\\d+p")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (r2MediaStorageService.isEnabled()) {
            if (r2MediaStorageService.isRelayEnabled()) {
                return hlsObject(episode.get(), quality + "/index.m3u8");
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(r2MediaStorageService.objectUri(episode.get(), quality + "/index.m3u8"))
                    .build();
        }

        // create path for quality playlist
        URI uri = URI.create(baseURI + "videos_hls/" + episode.get().getTitle() + "/" + quality + "/index.m3u8");
        Path path = Paths.get(uri);

        System.out.println("Debug - Quality playlist path: " + path);

        if (!Files.exists(path)) {
            System.out.println("Debug - Quality playlist file not found at: " + path);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                .body(resource);
    }
}
