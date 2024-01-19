package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.Headers;
import edu.stanford.protege.webprotege.ipc.pulsar.PulsarProducersManager;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-10
 */
@SpringBootTest
@Import(MockJwtDecoderConfiguration.class)
@DirtiesContext
public class RpcRequestProcessor_TestCase {

    private static final String STATUS_CODE_300_ERROR = """
                {
                    "statusCode" : 300
                }
            """;

    private static final String ACCESSTOKEN = "accesstoken";

    private static final UserId USER = UserId.valueOf("TheUser");

    private static final RpcRequest REQUEST = new RpcRequest(new RpcMethod("the.channel.name"), null);

    private RpcRequestProcessor processor;

    @Autowired
    private ObjectMapper objectMapper;

    private Supplier<Msg> replyMessageSupplier = () -> null;

    @Mock
    private Messenger messenger;

    @BeforeEach
    void setUp() {
        when(messenger.sendAndReceive(any(), any(), any()))
                .thenAnswer((Answer<CompletableFuture<Msg>>) invocationOnMock -> CompletableFuture.completedFuture(replyMessageSupplier.get()));
        processor = new RpcRequestProcessor(messenger, objectMapper);
    }

    @Test
    void shouldPropagateErrorHeaderValue() {
        var reply = Msg.withHeader(Headers.ERROR, STATUS_CODE_300_ERROR);
        replyMessageSupplier = () -> reply;
        var response = processRequest();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(300);
    }

    @Test
    void shouldReturnInternalServerErrorForBadErrorHeaderValue() {
        var reply = Msg.withHeader(Headers.ERROR, "A value that won't parse");
        replyMessageSupplier = () -> reply;
        var response = processRequest();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void shouldReturnGatewayTimeOutForMessageReplyTimeout() {
        when(messenger.sendAndReceive(anyString(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("Timeout")));
        var response = processRequest();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
    }

    @Test
    void shouldHandleRuntimeExceptionThrownByMessageHandler() {
        when(messenger.sendAndReceive(anyString(), any(byte[].class), any()))
                .thenThrow(new RuntimeException());
        var response = processRequest();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void shouldReturnMapOfValuesOfMessagePayload() {
        replyMessageSupplier = () -> Msg.withPayload("""
                                                        {
                                                            "a" : "b"
                                                        }
                                                       """);
        var response = processRequest();
        assertThat(response.result()).containsEntry("a", "b");
    }


    private RpcResponse processRequest()  {
        try {
            return processor.processRequest(REQUEST, ACCESSTOKEN, USER).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
