-- Single store for now: products are not tied to a merchant until merchant accounts exist.
-- TODO(merchants): backfill merchant_id and restore NOT NULL when login/multi-store lands.
ALTER TABLE products ALTER COLUMN merchant_id DROP NOT NULL;

-- Free text the owner types and the AI reads as-is, e.g. "40, 41, 42" / "أبيض، أسود"
ALTER TABLE products ADD COLUMN category VARCHAR(100);
ALTER TABLE products ADD COLUMN sizes    VARCHAR(200);
ALTER TABLE products ADD COLUMN colors   VARCHAR(200);
