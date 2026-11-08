package com.salesbot.conversation.api;

import com.salesbot.conversation.application.ConversationQueries;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard inbox: every chat the bot had, and the full thread of one chat. */
@RestController
@RequestMapping("/api/conversations")
class ConversationController {

    private final ConversationQueries queries;

    ConversationController(ConversationQueries queries) {
        this.queries = queries;
    }

    @GetMapping
    List<ConversationQueries.ConversationSummary> list() {
        return queries.list();
    }

    @GetMapping("/{id}")
    ConversationQueries.ConversationDetail get(@PathVariable UUID id) {
        return queries.get(id);
    }
}
