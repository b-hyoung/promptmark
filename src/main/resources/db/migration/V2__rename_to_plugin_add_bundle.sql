-- Migration V2: rename `asset` domain to `plugin` and add `bundle` (N:N with plugin)
-- Idempotent: safe to re-run on every boot. Handles two reentry cases:
--   (a) First boot:   assets exists with data, plugins absent → rename
--   (b) Later boots:  plugins exists, V1 has re-created an empty `assets` shell → drop the shell

SET search_path TO promptmark, public, extensions;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='promptmark' AND table_name='plugins') THEN
        -- (b) plugins is already the real table. Drop any leftover V1 `assets` shell.
        DROP TABLE IF EXISTS promptmark.asset_tags CASCADE;
        DROP TABLE IF EXISTS promptmark.assets CASCADE;
    ELSIF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='promptmark' AND table_name='assets') THEN
        -- (a) First boot path
        EXECUTE 'ALTER TABLE assets RENAME TO plugins';
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='promptmark' AND table_name='asset_tags') THEN
            EXECUTE 'ALTER TABLE asset_tags RENAME TO plugin_tags';
            EXECUTE 'ALTER TABLE plugin_tags RENAME COLUMN asset_id TO plugin_id';
        END IF;
    END IF;
END $$;

-- Indexes: rename if old name still exists (first boot), otherwise silently skip
ALTER INDEX IF EXISTS idx_assets_status_created RENAME TO idx_plugins_status_created;
ALTER INDEX IF EXISTS idx_assets_seller         RENAME TO idx_plugins_seller;
ALTER INDEX IF EXISTS idx_assets_title_trgm     RENAME TO idx_plugins_title_trgm;
ALTER INDEX IF EXISTS idx_assets_embedding      RENAME TO idx_plugins_embedding;

-- bundles + bundle_plugin (N:N) — pure CREATE IF NOT EXISTS, idempotent
CREATE TABLE IF NOT EXISTS bundles (
    id            BIGSERIAL PRIMARY KEY,
    curator_id    BIGINT REFERENCES users(id),
    slug          VARCHAR(40) UNIQUE NOT NULL,
    name          VARCHAR(100) NOT NULL,
    tagline       VARCHAR(200),
    story         TEXT,
    price         INTEGER NOT NULL CHECK (price >= 0),
    thumbnail     VARCHAR(255),
    status        VARCHAR(10) NOT NULL DEFAULT 'PUBLIC' CHECK (status IN ('PUBLIC','HIDDEN','DELETED')),
    view_count    INTEGER NOT NULL DEFAULT 0,
    embedding     vector(1536),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_bundles_status_created ON bundles(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bundles_name_trgm ON bundles USING gin (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_bundles_embedding ON bundles USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE TABLE IF NOT EXISTS bundle_plugin (
    bundle_id     BIGINT NOT NULL REFERENCES bundles(id) ON DELETE CASCADE,
    plugin_id     BIGINT NOT NULL REFERENCES plugins(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (bundle_id, plugin_id)
);
CREATE INDEX IF NOT EXISTS idx_bundle_plugin_plugin ON bundle_plugin(plugin_id);

-- order_items / downloads / reports polymorphic columns
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS target_type VARCHAR(10) NOT NULL DEFAULT 'PLUGIN';
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS target_id   BIGINT;
UPDATE order_items SET target_id = asset_id WHERE target_id IS NULL;
ALTER TABLE order_items ALTER COLUMN asset_id DROP NOT NULL;

ALTER TABLE downloads ADD COLUMN IF NOT EXISTS target_type VARCHAR(10) NOT NULL DEFAULT 'PLUGIN';
ALTER TABLE downloads ADD COLUMN IF NOT EXISTS target_id   BIGINT;
UPDATE downloads SET target_id = asset_id WHERE target_id IS NULL;
ALTER TABLE downloads ALTER COLUMN asset_id DROP NOT NULL;

ALTER TABLE reports ADD COLUMN IF NOT EXISTS target_type VARCHAR(10) NOT NULL DEFAULT 'PLUGIN';
ALTER TABLE reports ADD COLUMN IF NOT EXISTS target_id   BIGINT;
UPDATE reports SET target_id = asset_id WHERE target_id IS NULL;
ALTER TABLE reports ALTER COLUMN asset_id DROP NOT NULL;
