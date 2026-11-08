package com.salesbot.ai.echo;

import com.salesbot.ai.AiReply;
import com.salesbot.ai.AiReplyProvider;
import com.salesbot.ai.ReplyRequest;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Placeholder provider that repeats the message — proves the pipeline end to end without an AI key.
 * Asking for a photo ("صورة"، "وريني") sends the photos of the products named in the message
 * (or the first product that has one), so the photo flow can be tried locally too.
 */
@Component
@ConditionalOnProperty(name = "salesbot.ai.provider", havingValue = "echo", matchIfMissing = true)
class EchoReplyProvider implements AiReplyProvider {

    private static final Pattern WANTS_PHOTO = Pattern.compile("صور|ور[ّ]?ين|photo|picture", Pattern.CASE_INSENSITIVE);

    @Override
    public AiReply reply(ReplyRequest request) {
        String text = request.customerText() == null ? "" : request.customerText();
        return new AiReply("وصلتني رسالتك: " + text, photos(text, request.products()), null);
    }

    private static List<String> photos(String text, List<ReplyRequest.ProductFact> products) {
        if (!WANTS_PHOTO.matcher(text).find()) {
            return List.of();
        }
        List<ReplyRequest.ProductFact> withPhoto = products.stream().filter(ReplyRequest.ProductFact::hasPhoto).toList();
        List<String> named = withPhoto.stream()
                .map(ReplyRequest.ProductFact::name)
                .filter(name -> text.toLowerCase().contains(name.toLowerCase()))
                .toList();
        if (!named.isEmpty()) {
            return named;
        }
        return withPhoto.isEmpty() ? List.of() : List.of(withPhoto.get(0).name());
    }
}
