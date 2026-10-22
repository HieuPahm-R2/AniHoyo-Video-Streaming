-- =============================================================================
--  AniHoyo schema migration - media/URL columns
--
--  Why: several columns that store object-storage URLs (or user supplied text)
--  were declared with Hibernate's default length (VARCHAR(255)) or with MySQL's
--  `nvarchar(n)` alias. Two problems followed:
--
--    1. `VARCHAR(255)` is too short for the absolute URLs returned by SeaweedFS
--       and Cloudflare R2. With MySQL strict mode (STRICT_TRANS_TABLES) the insert
--       fails with "Data truncation: Data too long for column 'trailer' at row 1",
--       which the API used to report as a bogus 403.
--    2. MySQL maps `nvarchar(n)` to VARCHAR(n) CHARACTER SET utf8mb3, so those
--       columns silently lose 4-byte characters (emoji, some CJK) into '?'.
--
--  Hibernate (ddl-auto=update) does not rewrite the definition of a column that
--  already exists, so apply this file once per existing database:
--
--      podman exec -i anihoyo-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" anihoyo \
--          < deploy/mysql/migrations/002-widen-media-url-columns.sql
--
--  Fresh databases created by Hibernate already use these definitions (they match
--  the @Column annotations in com.HieuPahm.AniHoyo.model.entities). The statements
--  are idempotent, so running the file twice is harmless.
-- =============================================================================

-- ---------------------------------------------------------------- seasons ----
ALTER TABLE seasons
    MODIFY COLUMN season_name VARCHAR(150)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    MODIFY COLUMN description VARCHAR(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    MODIFY COLUMN thumb       VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    MODIFY COLUMN trailer     VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ------------------------------------------------------------------ films ----
ALTER TABLE films
    MODIFY COLUMN name      VARCHAR(150)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    MODIFY COLUMN studio    VARCHAR(150)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    MODIFY COLUMN thumbnail VARCHAR(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    MODIFY COLUMN slider    VARCHAR(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- --------------------------------------------------------------- episodes ----
ALTER TABLE episodes
    MODIFY COLUMN title     VARCHAR(150)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    MODIFY COLUMN file_path VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ------------------------------------------------------------ categories ----
ALTER TABLE categories
    MODIFY COLUMN name VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ------------------------------------------------------------------ users ----
ALTER TABLE users
    MODIFY COLUMN avatar VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ---------------------------------------------------------- notifications ----
ALTER TABLE notifications
    MODIFY COLUMN message VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
