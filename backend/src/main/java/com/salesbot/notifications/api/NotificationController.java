package com.salesbot.notifications.api;

import com.salesbot.notifications.application.NotificationFeed;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The dashboard bell polls this every few seconds. */
@RestController
@RequestMapping("/api/notifications")
class NotificationController {

    private final NotificationFeed feed;

    NotificationController(NotificationFeed feed) {
        this.feed = feed;
    }

    @GetMapping
    NotificationFeed.NotificationPage list(@RequestParam(required = false) Instant after) {
        return feed.since(after);
    }
}
