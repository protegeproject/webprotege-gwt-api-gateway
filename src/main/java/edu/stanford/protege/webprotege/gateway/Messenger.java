package edu.stanford.protege.webprotege.gateway;

import edu.stanford.protege.webprotege.common.UserId;

import java.util.concurrent.CompletableFuture;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-03-28
 */
public interface Messenger {

    CompletableFuture<Msg> sendAndReceive(RpcRequest request, String accessToken, byte[] payload, UserId userId);
}
