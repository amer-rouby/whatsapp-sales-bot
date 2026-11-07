package com.salesbot.ai.echo;

import com.salesbot.ai.AiReply;
import com.salesbot.ai.AiReplyProvider;
import com.salesbot.ai.ReplyRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Placeholder provider that repeats the message — proves the pipeline end to end. */
@Component
@ConditionalOnProperty(name = "salesbot.ai.provider", havingValue = "echo", matchIfMissing = true)
class EchoReplyProvider implements AiReplyProvider {

    @Override
    public AiReply reply(ReplyRequest request) {
        return AiReply.text("وصلتني رسالتك: " + request.customerText());
    }
}
