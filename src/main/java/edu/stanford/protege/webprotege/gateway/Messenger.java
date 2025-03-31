package edu.stanford.protege.webprotege.gateway;

import edu.stanford.protege.webprotege.common.UserId;

import java.util.concurrent.CompletableFuture;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-03-28
 */
public interface Messenger {

    /**
     * Sends a message that is associated with a request. The request is used to determined where to send the message to.
     * @param request The request that is used to determine the channel where the request message will be sent to.
     * @param accessToken The access token.
     * @param payload The message payload.
     * @param userId The user associated with the message request
     * @return A future that contains the response message.
     */
    CompletableFuture<Msg> sendAndReceive(RpcRequest request, String accessToken, byte[] payload, UserId userId);
}
