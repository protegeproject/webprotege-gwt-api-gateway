package edu.stanford.protege.webprotege.gateway;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-07-21
 */
public class DefaultMessageChannelMapper implements MessageChannelMapper {

    @Override
    public String getChannelName(RpcRequest action) {
        return action.methodName();
    }
}
