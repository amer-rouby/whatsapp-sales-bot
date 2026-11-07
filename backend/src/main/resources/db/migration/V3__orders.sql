-- Single store for now (see V2): orders are not tied to a merchant yet.
-- TODO(merchants): backfill merchant_id and restore NOT NULL when login/multi-store lands.
ALTER TABLE orders ALTER COLUMN merchant_id DROP NOT NULL;

-- true when an item did not match the catalog, so its price is unknown and a human must check it
ALTER TABLE orders ADD COLUMN needs_review BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN currency VARCHAR(3);

CREATE TABLE order_items (
    id            UUID PRIMARY KEY,
    order_id      UUID          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_name  VARCHAR(200)  NOT NULL,
    size          VARCHAR(50),
    color         VARCHAR(50),
    quantity      INT           NOT NULL CHECK (quantity > 0),
    -- copied from the catalog at order time; null when the product was not found
    unit_price    NUMERIC(12, 2),
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_orders_conversation ON orders (conversation_id, created_at);
