package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.MessageHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureJson
@AutoConfigureJsonTesters
@ImportAutoConfiguration
@Import(TestChannelBinderConfiguration.class)
class AsyncRemoteProcedureCallExecutor_Test {

    private static final String ACCESS_TOKEN_VALUE = "AccessTokenValue";

    @Autowired
    AsyncRemoteProcedureCallExecutor executor;

    @Mock
    private OAuth2AccessToken accessToken;

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    JacksonTester<RpcRequest> jacksonTester;


    @BeforeEach
    void setUp() {
        when(accessToken.getTokenValue()).thenReturn(ACCESS_TOKEN_VALUE);
    }

    @Test
    void shouldExecuteCall() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        var responseMono = executor.exec(new RpcRequest(new RpcMethod("CreateClasses"), new ObjectNode(new JsonNodeFactory(false))),
                      accessToken);
        var received = outputDestination.receive();
        var headers = received.getHeaders();
        var payloadBytes = received.getPayload();

        var request = jacksonTester.read(new ByteArrayInputStream(payloadBytes));
        System.out.println(request);
    }
}