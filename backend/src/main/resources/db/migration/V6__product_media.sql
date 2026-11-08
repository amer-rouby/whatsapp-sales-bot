-- Products of any kind: free options (each value may have its own price and stock) and specs, plus photos.

-- 1) Options + specs replace the fixed sizes/colors text
ALTER TABLE products ADD COLUMN options JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE products ADD COLUMN specs   JSONB NOT NULL DEFAULT '[]'::jsonb;

-- "40, 41، 42" → [{"value":"40","price":null,"inStock":true}, ...]; NULL when nothing is left after splitting
CREATE FUNCTION pg_temp.option_values(raw TEXT) RETURNS JSONB LANGUAGE sql IMMUTABLE AS $$
    SELECT jsonb_agg(jsonb_build_object('value', v, 'price', NULL, 'inStock', TRUE) ORDER BY ord)
    FROM (
        SELECT DISTINCT ON (lower(btrim(part))) btrim(part) AS v, ord
        FROM regexp_split_to_table(coalesce(raw, ''), E'[,،\\n]') WITH ORDINALITY AS t(part, ord)
        WHERE btrim(part) <> ''
        ORDER BY lower(btrim(part)), ord
    ) parts
$$;

UPDATE products p
SET options = coalesce((
    SELECT jsonb_agg(jsonb_build_object('name', o.name, 'values', o.vals) ORDER BY o.ord)
    FROM (
        SELECT 1 AS ord, 'المقاس' AS name, pg_temp.option_values(p.sizes) AS vals
        UNION ALL
        SELECT 2, 'اللون', pg_temp.option_values(p.colors)
    ) o
    WHERE o.vals IS NOT NULL
), '[]'::jsonb);

ALTER TABLE products DROP COLUMN sizes;
ALTER TABLE products DROP COLUMN colors;

-- 2) What the customer picked, in order: [{"name":"المقاس","value":"42"}, ...]
ALTER TABLE order_items ADD COLUMN options JSONB NOT NULL DEFAULT '[]'::jsonb;

UPDATE order_items
SET options = coalesce((
    SELECT jsonb_agg(jsonb_build_object('name', c.name, 'value', c.value) ORDER BY c.ord)
    FROM (
        SELECT 1 AS ord, 'المقاس' AS name, size AS value
        UNION ALL
        SELECT 2, 'اللون', color
    ) c
    WHERE c.value IS NOT NULL AND btrim(c.value) <> ''
), '[]'::jsonb);

ALTER TABLE order_items DROP COLUMN size;
ALTER TABLE order_items DROP COLUMN color;

-- 3) Photos: details here, bytes in their own table so listing products never reads them
CREATE TABLE product_images (
    id          UUID PRIMARY KEY,
    product_id  UUID        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    position    INT         NOT NULL,
    width       INT         NOT NULL,
    height      INT         NOT NULL,
    bytes       INT         NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_product_images_product ON product_images (product_id, position);

CREATE TABLE product_image_files (
    image_id  UUID  PRIMARY KEY REFERENCES product_images (id) ON DELETE CASCADE,
    data      BYTEA NOT NULL,
    thumb     BYTEA NOT NULL
);

-- 4) A bot message can be a product photo (shown in the inbox)
ALTER TABLE messages ADD COLUMN image_id UUID;

-- 5) WhatsApp media ids for photos already uploaded to Meta (they expire after 30 days)
CREATE TABLE wa_media_cache (
    cache_key        VARCHAR(100) NOT NULL,
    phone_number_id  VARCHAR(64)  NOT NULL,
    media_id         VARCHAR(100) NOT NULL,
    uploaded_at      TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (cache_key, phone_number_id)
);
