package com.salesbot.ai.gemini;

import com.salesbot.ai.AiReply;
import com.salesbot.ai.AiReplyProvider;
import com.salesbot.ai.AiUnavailableException;
import com.salesbot.ai.ReplyRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Replies through Google's Gemini API ({@code salesbot.ai.provider=gemini}). */
@Component
@ConditionalOnProperty(name = "salesbot.ai.provider", havingValue = "gemini")
class GeminiReplyProvider implements AiReplyProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiReplyProvider.class);
    private static final String USER = "user";
    private static final String MODEL = "model";

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final String systemPrompt;
    private final JsonMapper jsonMapper;

    GeminiReplyProvider(RestClient geminiRestClient, GeminiProperties properties, SystemPromptLoader promptLoader,
                        JsonMapper jsonMapper) {
        this.restClient = geminiRestClient;
        this.properties = properties;
        this.systemPrompt = promptLoader.load(properties.systemPrompt());
        this.jsonMapper = jsonMapper;
    }

    @Override
    public AiReply reply(ReplyRequest request) {
        GeminiApi.GenerateRequest body = new GeminiApi.GenerateRequest(
                GeminiApi.Content.of(null, systemPrompt
                        + "\n\n" + CatalogPromptFormatter.format(request.products())
                        + customerLine(request)),
                toContents(request),
                new GeminiApi.GenerationConfig(properties.temperature(), thinkingConfig(),
                        "application/json", StructuredReply.SCHEMA));

        return parse(extractText(callWithFallback(body)));
    }

    /** Tries each configured model in order; moves on only for quota/outage errors, not for bad requests. */
    private GeminiApi.GenerateResponse callWithFallback(GeminiApi.GenerateRequest body) {
        RestClientException last = null;
        for (String model : properties.models()) {
            try {
                return restClient.post()
                        .uri("/models/{model}:generateContent", model)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(GeminiApi.GenerateResponse.class);
            } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException | ResourceAccessException e) {
                last = e;
                log.warn("Gemini model {} unavailable ({}), trying the next one", model, summary(e));
            } catch (RestClientException e) {
                throw new AiUnavailableException("Gemini call failed on " + model + ": " + e.getMessage(), e);
            }
        }
        throw new AiUnavailableException("All Gemini models unavailable: " + summary(last), last);
    }

    private static String summary(RestClientException e) {
        if (e instanceof RestClientResponseException r) {
            return r.getStatusCode().toString();
        }
        return e == null ? "none configured" : e.getClass().getSimpleName();
    }

    /** Turns the model's JSON into an {@link AiReply}; anything malformed counts as "AI unavailable". */
    AiReply parse(String json) {
        StructuredReply structured;
        try {
            structured = jsonMapper.readValue(json, StructuredReply.class);
        } catch (JacksonException e) {
            throw new AiUnavailableException("Gemini returned invalid JSON", e);
        }
        if (structured.reply() == null || structured.reply().isBlank()) {
            throw new AiUnavailableException("Gemini returned an empty reply");
        }
        return new AiReply(structured.reply().strip(), toDraft(structured.order()));
    }

    private static AiReply.OrderDraft toDraft(StructuredReply.Order order) {
        if (order == null || order.items() == null) {
            return null;
        }
        List<AiReply.OrderDraft.Item> items = order.items().stream()
                .filter(i -> i != null && i.productName() != null && !i.productName().isBlank())
                .map(i -> new AiReply.OrderDraft.Item(i.productName().strip(), blankToNull(i.size()),
                        blankToNull(i.color()), i.quantity() == null || i.quantity() < 1 ? 1 : i.quantity()))
                .toList();
        return items.isEmpty() ? null : new AiReply.OrderDraft(blankToNull(order.customerName()),
                blankToNull(order.address()), items);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /** Omitted when blank, so models without thinking levels keep working. */
    private GeminiApi.ThinkingConfig thinkingConfig() {
        String level = properties.thinkingLevel();
        return level == null || level.isBlank() ? null : new GeminiApi.ThinkingConfig(level);
    }

    private static String customerLine(ReplyRequest request) {
        String name = request.customerName();
        return name == null || name.isBlank() ? "" : "\n\nاسم العميل على واتساب: " + name;
    }

    /**
     * History + the new message as alternating user/model turns.
     * Consecutive turns with the same role are merged, since the API expects alternation.
     */
    static List<GeminiApi.Content> toContents(ReplyRequest request) {
        List<String[]> turns = new ArrayList<>();
        for (ReplyRequest.Turn turn : request.history()) {
            turns.add(new String[] {turn.role() == ReplyRequest.Role.CUSTOMER ? USER : MODEL, turn.text()});
        }
        turns.add(new String[] {USER, request.customerText()});

        List<GeminiApi.Content> contents = new ArrayList<>();
        String currentRole = null;
        StringBuilder currentText = new StringBuilder();
        for (String[] turn : turns) {
            if (turn[0].equals(currentRole)) {
                currentText.append('\n').append(turn[1]);
                continue;
            }
            if (currentRole != null) {
                contents.add(GeminiApi.Content.of(currentRole, currentText.toString()));
            }
            currentRole = turn[0];
            currentText = new StringBuilder(turn[1]);
        }
        contents.add(GeminiApi.Content.of(currentRole, currentText.toString()));

        // A conversation must start with the customer; drop a leading bot turn if history was cut there
        if (MODEL.equals(contents.get(0).role())) {
            contents.remove(0);
        }
        return contents;
    }

    private static String extractText(GeminiApi.GenerateResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new AiUnavailableException("Gemini returned no candidates");
        }
        GeminiApi.Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null) {
            throw new AiUnavailableException("Gemini returned an empty answer (finishReason="
                    + response.candidates().get(0).finishReason() + ")");
        }
        String text = content.parts().stream()
                .map(GeminiApi.Part::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining())
                .strip();
        if (text.isEmpty()) {
            throw new AiUnavailableException("Gemini returned blank text");
        }
        return text;
    }
}
