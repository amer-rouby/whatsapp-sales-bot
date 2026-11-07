package com.salesbot.ai;

/** Produces the bot's reply to a customer message. */
public interface AiReplyProvider {

    /**
     * @throws AiUnavailableException when no reply could be produced
     */
    AiReply reply(ReplyRequest request);
}
