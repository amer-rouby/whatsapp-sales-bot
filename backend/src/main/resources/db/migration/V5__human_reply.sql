-- The shop owner can take over a chat from the dashboard: the bot stays quiet there until resumed.
ALTER TABLE conversations ADD COLUMN bot_paused BOOLEAN NOT NULL DEFAULT FALSE;

-- Who wrote each message: the customer, the bot, or a person from the shop (AGENT).
ALTER TABLE messages ADD COLUMN author VARCHAR(10);
UPDATE messages SET author = CASE direction WHEN 'INBOUND' THEN 'CUSTOMER' ELSE 'BOT' END;
ALTER TABLE messages ALTER COLUMN author SET NOT NULL;

-- The 24-hour WhatsApp window check reads the customer's latest message.
CREATE INDEX idx_messages_conversation_author ON messages (conversation_id, author, created_at);
