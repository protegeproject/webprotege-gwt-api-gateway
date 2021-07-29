package edu.stanford.protege.webprotege.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SpringBootTest
class MessageChannelMapper_Test {

    private static final String THE_METHOD_NAME = "TheMethodName";

    @Autowired
    private MessageChannelMapper mapper;

    @Test
    void shouldUseMethodNameAsChannelName() {
        var channelName = mapper.getChannelName(new RpcRequest(new RpcMethod(THE_METHOD_NAME),
                                             null));
        assertThat(channelName, is(THE_METHOD_NAME));
    }


}