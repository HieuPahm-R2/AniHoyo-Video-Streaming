-- =============================================================================
--  AniHoyo schema migration - ACL data fixes
--
--  The `permissions` table is compared literally with the request path, so a
--  missing or mistyped row means the endpoint can never be called by anybody
--  (the ACL denies by default) while an anonymous caller is unaffected. Two rows
--  were wrong:
--
--    * DELETE /api/v1/delete-episode/{id}  was stored as
--      "/api/v1/delete-epsiode/{id}" (typo) -> deleting an episode always 403.
--    * GET /api/v1/seasons/related/{seasonId} had no row at all -> the "related
--      seasons" block of the watch page always 403 for a logged-in visitor.
--
--  Run once per existing database (idempotent):
--
--      podman exec -i anihoyo-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" anihoyo \
--          < deploy/mysql/migrations/003-sync-permission-paths.sql
--
--  Fresh databases are seeded correctly by DatabaseInitializer.
-- =============================================================================

-- 1. Fix the typo. The row keeps its id, so every role that had the broken
--    permission automatically gets the working one.
UPDATE permissions
SET api_path = '/api/v1/delete-episode/{id}',
    name     = 'Delete a episode'
WHERE api_path = '/api/v1/delete-epsiode/{id}'
  AND NOT EXISTS (
      SELECT 1 FROM (SELECT id FROM permissions WHERE api_path = '/api/v1/delete-episode/{id}') AS existing
  );

-- 2. Create the missing "related seasons" permission once.
INSERT INTO permissions (name, api_path, method, module, created_by, created_time)
SELECT 'fetch seasons related', '/api/v1/seasons/related/{seasonId}', 'GET', 'SEASONS', 'migration', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT id FROM permissions WHERE api_path = '/api/v1/seasons/related/{seasonId}') AS existing
);

-- 3. Grant it to every role that can already list seasons, so non-admin roles do
--    not silently lose a feature they could use before.
INSERT INTO permission_role (role_id, permission_id)
SELECT DISTINCT holder.role_id, related.id
FROM permission_role holder
         JOIN permissions sibling ON sibling.id = holder.permission_id
         JOIN permissions related ON related.api_path = '/api/v1/seasons/related/{seasonId}'
WHERE sibling.api_path = '/api/v1/seasons'
  AND sibling.method = 'GET'
  AND NOT EXISTS (
      SELECT 1 FROM (SELECT role_id, permission_id FROM permission_role) AS granted
      WHERE granted.role_id = holder.role_id
        AND granted.permission_id = related.id
  );
