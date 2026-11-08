package com.salesbot.conversation.api;

import com.salesbot.conversation.application.AgentReplies;
import com.salesbot.conversation.application.ConversationQueries;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard inbox: every chat, one chat's thread, and the shop answering a customer itself. */
@RestController
@RequestMapping("/api/conversations")
class ConversationController {

    private final ConversationQueries queries;
    private final AgentReplies agentReplies;

    ConversationController(ConversationQueries queries, AgentReplies agentReplies) {
        this.queries = queries;
        this.agentReplies = agentReplies;
    }

    @GetMapping
    List<ConversationQueries.ConversationSummary> list() {
        return queries.list();
    }

    @GetMapping("/{id}")
    ConversationQueries.ConversationDetail get(@PathVariable UUID id) {
        return queries.get(id);
    }

    /** Sends the shop's reply on WhatsApp and pauses the bot in this chat. */
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    ConversationQueries.ChatMessage reply(@PathVariable UUID id, @Valid @RequestBody AgentReplyRequest request) {
        return agentReplies.send(id, request.text());
    }

    /** {@code paused=false} hands the chat back to the bot. */
    @PutMapping("/{id}/bot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setBot(@PathVariable UUID id, @Valid @RequestBody BotStateRequest request) {
        agentReplies.setBotPaused(id, request.paused());
    }

    record AgentReplyRequest(@NotBlank @Size(max = 4096) String text) {
    }

    record BotStateRequest(@NotNull Boolean paused) {
    }
}
