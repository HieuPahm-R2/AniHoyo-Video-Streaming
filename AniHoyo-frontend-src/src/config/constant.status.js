/**
 * Giá trị enum của backend dùng chung cho form Season và các bộ lọc phía client.
 *
 * Nguồn sự thật (backend):
 *   - AniHoyo-backend-server/src/main/java/com/HieuPahm/AniHoyo/utils/constant/GenersEnum.java  -> SEASON_TYPES
 *   - AniHoyo-backend-server/src/main/java/com/HieuPahm/AniHoyo/utils/constant/StatusEnum.java  -> SEASON_STATUS
 *
 * Vì sao hardcode ở FE là chấp nhận được: hợp đồng ghi của Season truyền **tên enum**
 * (SeasonDTO.type / SeasonDTO.status), không truyền id do server sinh — khác với
 * Category/Tag (FK theo id, phải lấy từ API).
 *
 * ⚠️ Đổi tên/ thêm giá trị trong 2 enum bên backend thì PHẢI sửa ở đây, nếu không
 * request sẽ lỗi deserialize (400) hoặc filter trả rỗng mà không có cảnh báo nào.
 */

export const SEASON_TYPES = {
  SERIES: 'SERIES',
  MOVIE: 'MOVIE',
  OVA: 'OVA',
  SPECIAL: 'SPECIAL',
};

export const SEASON_TYPE_OPTIONS = [
  { value: SEASON_TYPES.SERIES, label: 'SERIES' },
  { value: SEASON_TYPES.MOVIE, label: 'MOVIE' },
  { value: SEASON_TYPES.OVA, label: 'OVA' },
  { value: SEASON_TYPES.SPECIAL, label: 'SPECIAL' },
];

export const SEASON_STATUS = {
  ON_AIR: 'ON_AIR',
  CANCEL: 'CANCEL',
  COMMING_SOON: 'COMMING_SOON',
  DELAY: 'DELAY',
  FINISHED: 'FINISHED',
};

export const SEASON_STATUS_OPTIONS = [
  { value: SEASON_STATUS.ON_AIR, label: 'Đang Chiếu' },
  { value: SEASON_STATUS.CANCEL, label: 'Bị Hủy' },
  { value: SEASON_STATUS.COMMING_SOON, label: 'Sắp Chiếu' },
  { value: SEASON_STATUS.DELAY, label: 'Bị Hoãn' },
  { value: SEASON_STATUS.FINISHED, label: 'Hoàn thành' },
];
