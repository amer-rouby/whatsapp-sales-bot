package com.salesbot.messaging.infrastructure;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Remembers which photos were already uploaded to WhatsApp, per business number. Meta keeps an uploaded file
 * for 30 days; ids older than {@link #REUSE_FOR} are treated as gone and the photo is uploaded again.
 */
@Component
class MediaCache {

    static final Duration REUSE_FOR = Duration.ofDays(25);

    private final JdbcTemplate jdbc;

    MediaCache(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Optional<String> find(String cacheKey, String phoneNumberId) {
        return jdbc.query("""
                        SELECT media_id FROM wa_media_cache
                        WHERE cache_key = ? AND phone_number_id = ? AND uploaded_at > ?""",
                (rs, row) -> rs.getString(1),
                cacheKey, phoneNumberId, Timestamp.from(Instant.now().minus(REUSE_FOR))).stream().findFirst();
    }

    void remember(String cacheKey, String phoneNumberId, String mediaId) {
        jdbc.update("""
                        INSERT INTO wa_media_cache (cache_key, phone_number_id, media_id, uploaded_at)
                        VALUES (?, ?, ?, now())
                        ON CONFLICT (cache_key, phone_number_id)
                        DO UPDATE SET media_id = EXCLUDED.media_id, uploaded_at = EXCLUDED.uploaded_at""",
                cacheKey, phoneNumberId, mediaId);
    }

    void forget(String cacheKey, String phoneNumberId) {
        jdbc.update("DELETE FROM wa_media_cache WHERE cache_key = ? AND phone_number_id = ?", cacheKey, phoneNumberId);
    }
}
