-- promptmark schema v1
-- Apply once per database. All statements are IF NOT EXISTS / CREATE TABLE so re-runs are safe.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(120) UNIQUE NOT NULL,
  password_hash VARCHAR(120) NOT NULL,
  nickname      VARCHAR(40)  NOT NULL,
  role          VARCHAR(10)  NOT NULL CHECK (role IN ('USER','SELLER','ADMIN')),
  status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE','BANNED')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assets (
  id             BIGSERIAL PRIMARY KEY,
  seller_id      BIGINT NOT NULL REFERENCES users(id),
  type           VARCHAR(10) NOT NULL CHECK (type IN ('PROMPT','MD')),
  title          VARCHAR(120) NOT NULL,
  summary        VARCHAR(300) NOT NULL,
  body           TEXT,
  file_key       VARCHAR(200),
  demo_url       VARCHAR(500),
  video_url      VARCHAR(500),
  price          INTEGER NOT NULL DEFAULT 0 CHECK (price >= 0),
  status         VARCHAR(10) NOT NULL DEFAULT 'PUBLIC'
                 CHECK (status IN ('PUBLIC','HIDDEN','DELETED')),
  view_count     INTEGER NOT NULL DEFAULT 0,
  download_count INTEGER NOT NULL DEFAULT 0,
  embedding      vector(1536),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_assets_status_created ON assets(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_assets_seller         ON assets(seller_id);
CREATE INDEX IF NOT EXISTS idx_assets_title_trgm     ON assets USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_assets_embedding
       ON assets USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE TABLE IF NOT EXISTS tags (
  id   BIGSERIAL PRIMARY KEY,
  name VARCHAR(40) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS asset_tags (
  asset_id BIGINT REFERENCES assets(id) ON DELETE CASCADE,
  tag_id   BIGINT REFERENCES tags(id)   ON DELETE CASCADE,
  PRIMARY KEY (asset_id, tag_id)
);

CREATE TABLE IF NOT EXISTS orders (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES users(id),
  total_amount INTEGER NOT NULL CHECK (total_amount >= 0),
  status       VARCHAR(10) NOT NULL CHECK (status IN ('PAID','CANCELED')),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS order_items (
  id         BIGSERIAL PRIMARY KEY,
  order_id   BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  asset_id   BIGINT NOT NULL REFERENCES assets(id),
  price_paid INTEGER NOT NULL CHECK (price_paid >= 0),
  UNIQUE (order_id, asset_id)
);

CREATE TABLE IF NOT EXISTS downloads (
  id            BIGSERIAL PRIMARY KEY,
  user_id       BIGINT NOT NULL REFERENCES users(id),
  asset_id      BIGINT NOT NULL REFERENCES assets(id),
  downloaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_downloads_user ON downloads(user_id, downloaded_at DESC);

CREATE TABLE IF NOT EXISTS reports (
  id          BIGSERIAL PRIMARY KEY,
  asset_id    BIGINT NOT NULL REFERENCES assets(id),
  reporter_id BIGINT NOT NULL REFERENCES users(id),
  reason      VARCHAR(300) NOT NULL,
  status      VARCHAR(10) NOT NULL DEFAULT 'OPEN'
              CHECK (status IN ('OPEN','RESOLVED','REJECTED')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
