package com.salesbot.messaging.infrastructure;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.salesbot.messaging.OutboundImage;
import com.salesbot.messaging.simulator.SimulatorOutbox;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Photos go to Meta once and are then sent by media id, re-uploaded when Meta no longer has them. */
class CloudApiMessageSenderTest {

    private static final String BASE = "https://graph.test/v99";
    private static final OutboundImage PHOTO = new OutboundImage("product-image:1", "image/jpeg", new byte[] {1, 2, 3}, "ساعة");

    private MockRestServiceServer meta;
    private MediaCache cache;
    private CloudApiMessageSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        meta = MockRestServiceServer.bindTo(builder).build();
        cache = mock(MediaCache.class);
        sender = new CloudApiMessageSender(builder,
                new WhatsAppProperties(null, null, true, "token", "https://graph.test", "v99", "cloud"),
                new SimulatorOutbox(), cache);
    }

    @Test
    void uploadsOnceThenSendsByMediaId() {
        when(cache.find("product-image:1", "PN")).thenReturn(Optional.empty());
        meta.expect(requestTo(BASE + "/PN/media")).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess("{\"id\":\"MEDIA-1\"}", MediaType.APPLICATION_JSON));
        meta.expect(requestTo(BASE + "/PN/messages"))
                .andExpect(jsonPath("$.type").value("image"))
                .andExpect(jsonPath("$.image.id").value("MEDIA-1"))
                .andExpect(jsonPath("$.image.caption").value("ساعة"))
                .andRespond(withSuccess());

        sender.sendImage("PN", "2010", PHOTO);

        meta.verify();
        verify(cache).remember("product-image:1", "PN", "MEDIA-1");
    }

    @Test
    void reusesACachedUpload() {
        when(cache.find("product-image:1", "PN")).thenReturn(Optional.of("MEDIA-OLD"));
        meta.expect(requestTo(BASE + "/PN/messages"))
                .andExpect(jsonPath("$.image.id").value("MEDIA-OLD"))
                .andRespond(withSuccess());

        sender.sendImage("PN", "2010", PHOTO);

        meta.verify();
        verify(cache, never()).remember("product-image:1", "PN", "MEDIA-OLD");
    }

    @Test
    void uploadsAgainWhenMetaRefusesTheCachedId() {
        when(cache.find("product-image:1", "PN")).thenReturn(Optional.of("MEDIA-GONE"));
        meta.expect(requestTo(BASE + "/PN/messages"))
                .andExpect(jsonPath("$.image.id").value("MEDIA-GONE"))
                .andRespond(withBadRequest());
        meta.expect(requestTo(BASE + "/PN/media"))
                .andRespond(withSuccess("{\"id\":\"MEDIA-NEW\"}", MediaType.APPLICATION_JSON));
        meta.expect(requestTo(BASE + "/PN/messages"))
                .andExpect(jsonPath("$.image.id").value("MEDIA-NEW"))
                .andRespond(withSuccess());

        sender.sendImage("PN", "2010", PHOTO);

        meta.verify();
        verify(cache).forget("product-image:1", "PN");
        verify(cache).remember("product-image:1", "PN", "MEDIA-NEW");
    }
}
