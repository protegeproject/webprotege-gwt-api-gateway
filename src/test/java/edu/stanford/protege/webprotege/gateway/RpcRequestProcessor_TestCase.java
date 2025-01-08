package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-10
 */
@ExtendWith(MockitoExtension.class)
class RpcRequestProcessor_TestCase {

    private static final String STATUS_CODE_300_ERROR = """
                {
                    "statusCode" : 300
                }
            """;

    private static final String ACCESSTOKEN = "accesstoken";

    private static final UserId USER = UserId.valueOf("TheUser");

    private static final RpcRequest REQUEST = new RpcRequest(new RpcMethod("the.channel.name"), null);

    private RpcRequestProcessor processor;

    private Supplier<Msg> replyMessageSupplier = () -> null;

    @Mock
    private Messenger messenger;

    @BeforeEach
    void setUp() {
        var objectMapper = new ObjectMapper();
        when(messenger.sendAndReceive(any(), any(), any(), any()))
                .thenAnswer((Answer<CompletableFuture<Msg>>) invocationOnMock -> CompletableFuture.completedFuture(replyMessageSupplier.get()));
        processor = new RpcRequestProcessor("AppName", messenger, objectMapper);
    }

    @Test
    void shouldPropagateErrorHeaderValue() throws Exception {
        var reply = Msg.withHeader(Headers.ERROR, STATUS_CODE_300_ERROR);
        replyMessageSupplier = () -> reply;
        var response = processRequest();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(300);
    }

    @Test
    void shouldThrowInternalServerErrorForErrorHeaderThatIsUnparseable() throws Exception {
        var reply = Msg.withHeader(Headers.ERROR, "A value that won't parse");
        replyMessageSupplier = () -> reply;
        try {
            var response = processRequest();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ResponseStatusException.class);
            ResponseStatusException ex = (ResponseStatusException) e.getCause();
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @Test
    void shouldHandleLeakyRuntimeExceptionThrownByMessageHandler() throws Exception {
        var testMsg = "Fail as part of test";
        when(messenger.sendAndReceive(any(),anyString(), any(byte[].class), any()))
                .thenThrow(new RuntimeException(testMsg));
        try {
            processRequest();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ResponseStatusException.class);
            ResponseStatusException ex = (ResponseStatusException) e.getCause();
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(ex.getMessage()).contains(testMsg);
        }
    }

   @Test
    void shouldPropagateFailedFutureFromMessengerAsInternalServerError() throws Exception {
        var testMsg = "Fail as part of message send";
        when(messenger.sendAndReceive(any(),anyString(), any(byte[].class), any()))
                .thenReturn(CompletableFuture.failedFuture(new AmqpTimeoutException(testMsg)));
        try {
            processRequest();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ResponseStatusException.class);
            ResponseStatusException ex = (ResponseStatusException) e.getCause();
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(ex.getMessage()).contains(testMsg);
        }
    }

    @Test
    void shouldReturnMapOfValuesOfMessagePayload() throws Exception {
        replyMessageSupplier = () -> Msg.withPayload("""
                                                        {
                                                            "a" : "b"
                                                        }
                                                       """);
        var response = processRequest();
        assertThat(response.result()).containsEntry("a", "b");
    }


    private RpcResponse processRequest() throws Exception {
        return processor.processRequest(REQUEST, ACCESSTOKEN, USER).get();
    }
}
