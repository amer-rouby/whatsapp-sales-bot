package com.salesbot.ai.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.salesbot.ai.AiReply;
import com.salesbot.ai.AiUnavailableException;
import com.salesbot.ai.ReplyRequest;
import com.salesbot.ai.ReplyRequest.ProductFact;
import com.salesbot.ai.ReplyRequest.Role;
import com.salesbot.ai.ReplyRequest.Turn;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class GeminiReplyProviderTest {

    private static final String URL = "https://gemini.test/v1beta/models/gemini-test:generateContent";
    private static final String BACKUP_URL = "https://gemini.test/v1beta/models/gemini-backup:generateContent";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private MockRestServiceServer server;
    private GeminiReplyProvider provider;

    @BeforeEach
    void setUp() {
        GeminiProperties properties = new GeminiProperties(
                "test-key", List.of("gemini-test", "gemini-backup"), "https://gemini.test/v1beta",
                Duration.ofSeconds(5), 0.6,
                "low", "classpath:prompts/sales-assistant-ar.txt");
        // real production setup (URL, key header), then the mock replaces only the HTTP transport
        RestClient.Builder builder = GeminiConfig.configure(RestClient.builder(), properties);
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new GeminiReplyProvider(builder.build(), properties,
                new SystemPromptLoader(new DefaultResourceLoader()), JSON);
    }

    /** A Gemini HTTP response whose text part is the given structured answer. */
    private static ResponseCreator answer(Object structured) {
        String text = JSON.writeValueAsString(structured);
        String body = JSON.writeValueAsString(Map.of("candidates", List.of(Map.of(
                "content", Map.of("role", "model", "parts", List.of(Map.of("text", text))),
                "finishReason", "STOP"))));
        return withSuccess(body, MediaType.APPLICATION_JSON);
    }

    private static ReplyRequest ask(String text) {
        return new ReplyRequest(null, text, List.of(), List.of());
    }

    @Test
    void sendsPromptCatalogHistoryAndSchema() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value(allOf(
                        containsString("موظف مبيعات"),
                        containsString(CatalogPromptFormatter.HEADER),
                        containsString("- «كوتشي أبيض» | القسم: أحذية | السعر: 1250 EGP | المقاسات: 40, 41, 42 | الألوان: أبيض | متاح"),
                        containsString("- «صندل صيفي» | السعر: 650 EGP | خلص (مش متاح دلوقتي)"))))
                .andExpect(jsonPath("$.generationConfig.thinkingConfig.thinkingLevel").value("low"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseJsonSchema.required[0]").value("reply"))
                .andExpect(jsonPath("$.contents.length()").value(3))
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[1].role").value("model"))
                .andExpect(jsonPath("$.contents[2].parts[0].text").value("عندكم مقاس 42؟"))
                .andRespond(answer(Map.of("reply", " أيوه متاح يا فندم 👟 ")));

        AiReply reply = provider.reply(new ReplyRequest("Ahmed", "عندكم مقاس 42؟", List.of(
                new Turn(Role.CUSTOMER, "السلام عليكم"),
                new Turn(Role.BOT, "وعليكم السلام، أؤمرني")), List.of(
                new ProductFact("كوتشي أبيض", "أحذية", null, new BigDecimal("1250.00"), "EGP",
                        "40, 41, 42", "أبيض", true),
                new ProductFact("صندل صيفي", null, null, new BigDecimal("650.00"), "EGP", null, null, false))));

        assertThat(reply.text()).isEqualTo("أيوه متاح يا فندم 👟");
        assertThat(reply.order()).isNull();
        server.verify();
    }

    @Test
    void readsTheConfirmedOrder() {
        server.expect(requestTo(URL)).andRespond(answer(Map.of(
                "reply", "تم تأكيد الأوردر",
                "order", Map.of(
                        "customerName", "عامر",
                        "address", " المنيا، سمالوط ",
                        "items", List.of(
                                Map.of("productName", " كوتشي أبيض ", "size", "42", "color", "أبيض", "quantity", 2),
                                Map.of("productName", "شراب", "quantity", 0),   // quantity fixed to 1
                                Map.of("productName", " ", "quantity", 1))))));  // blank name dropped

        AiReply reply = provider.reply(ask("أيوه أكد"));

        assertThat(reply.text()).isEqualTo("تم تأكيد الأوردر");
        assertThat(reply.order().customerName()).isEqualTo("عامر");
        assertThat(reply.order().address()).isEqualTo("المنيا، سمالوط");
        assertThat(reply.order().items()).containsExactly(
                new AiReply.OrderDraft.Item("كوتشي أبيض", "42", "أبيض", 2),
                new AiReply.OrderDraft.Item("شراب", null, null, 1));
    }

    @Test
    void invalidJsonBecomesAiUnavailable() {
        assertThatThrownBy(() -> provider.parse("not json"))
                .isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> provider.parse("{\"reply\":\"  \",\"order\":null}"))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void outOfQuotaModelFallsBackToTheNextOne() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(BACKUP_URL)).andRespond(answer(Map.of("reply", "رد من الموديل الاحتياطي")));

        assertThat(provider.reply(ask("بكام؟")).text()).isEqualTo("رد من الموديل الاحتياطي");
        server.verify();
    }

    @Test
    void allModelsOutOfQuotaBecomesAiUnavailable() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(BACKUP_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> provider.reply(ask("بكام؟")))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("All Gemini models unavailable");
    }

    @Test
    void badRequestDoesNotTryOtherModels() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> provider.reply(ask("بكام؟"))).isInstanceOf(AiUnavailableException.class);
        server.verify(); // the backup was never called
    }

    @Test
    void emptyCatalogTellsTheModelNothingIsAvailable() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value(
                        containsString(CatalogPromptFormatter.EMPTY)))
                .andRespond(answer(Map.of("reply", "هتأكد وأرد عليك")));

        assertThat(provider.reply(ask("عندكم شنط؟")).text()).isEqualTo("هتأكد وأرد عليك");
        server.verify();
    }

    @Test
    void blockedOrEmptyAnswerBecomesAiUnavailable() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.reply(ask("بكام؟")))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("SAFETY");
    }

    @Test
    void mergesSameRoleTurnsAndDropsLeadingBotTurn() {
        List<GeminiApi.Content> contents = GeminiReplyProvider.toContents(new ReplyRequest(null, "تالت رسالة", List.of(
                new Turn(Role.BOT, "رد قديم"),
                new Turn(Role.CUSTOMER, "أول رسالة"),
                new Turn(Role.CUSTOMER, "تاني رسالة")), List.of()));

        assertThat(contents).hasSize(1);
        assertThat(contents.get(0).role()).isEqualTo("user");
        assertThat(contents.get(0).parts().get(0).text()).isEqualTo("أول رسالة\nتاني رسالة\nتالت رسالة");
    }
}
