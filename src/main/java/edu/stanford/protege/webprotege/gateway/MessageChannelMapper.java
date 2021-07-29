package edu.stanford.protege.webprotege.gateway;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-21
 */
public interface MessageChannelMapper {

    String getChannelName(RpcRequest action);
}
