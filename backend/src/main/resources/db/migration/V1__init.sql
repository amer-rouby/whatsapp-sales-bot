-- Merchants: one per WhatsApp business number
CREATE TABLE merchants (
    id               UUID PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    phone_number_id  VARCHAR(64)  NOT NULL UNIQUE,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE TABLE products (
    id           UUID PRIMARY KEY,
    merchant_id  UUID          NOT NULL REFERENCES merchants (id),
    name         VARCHAR(200)  NOT NULL,
    description  TEXT,
    price        NUMERIC(12, 2) NOT NULL,
    currency     VARCHAR(3)    NOT NULL,
    in_stock     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_products_merchant ON products (merchant_id);

-- Conversations are keyed by the business number + customer number
CREATE TABLE conversations (
    id               UUID PRIMARY KEY,
    phone_number_id  VARCHAR(64)  NOT NULL,
    customer_wa_id   VARCHAR(32)  NOT NULL,
    customer_name    VARCHAR(200),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_conversation UNIQUE (phone_number_id, customer_wa_id)
);

CREATE TABLE messages (
    id               UUID PRIMARY KEY,
    conversation_id  UUID         NOT NULL REFERENCES conversations (id),
    direction        VARCHAR(10)  NOT NULL,
    -- WhatsApp message id; unique so webhook retries are ignored
    wa_message_id    VARCHAR(128) UNIQUE,
    body             TEXT         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at);

CREATE TABLE orders (
    id                UUID PRIMARY KEY,
    merchant_id       UUID          NOT NULL REFERENCES merchants (id),
    conversation_id   UUID          REFERENCES conversations (id),
    customer_name     VARCHAR(200),
    customer_phone    VARCHAR(32)   NOT NULL,
    shipping_address  TEXT,
    status            VARCHAR(20)   NOT NULL,
    total             NUMERIC(12, 2),
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_orders_merchant ON orders (merchant_id, created_at);
