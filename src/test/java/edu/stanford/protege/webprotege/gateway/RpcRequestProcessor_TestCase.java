package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.requestreply.KafkaReplyTimeoutException;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-10
 */
@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
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

    @Mock
    private MessageHandler messageHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private Supplier<Message<String>> replyMessageSupplier = () -> null;

    @BeforeEach
    void setUp() {
        when(messageHandler.sendAndReceive(any(), any()))
                .thenAnswer(inv -> CompletableFuture.completedFuture(replyMessageSupplier.get()));
        processor = new RpcRequestProcessor(messageHandler, objectMapper, "the-reply-channel", Duration.ofSeconds(2));
    }

    @Test
    void shouldPropagateErrorHeaderValue() {
        var reply = MessageBuilder.withPayload("")
                                  .setHeader(Headers.ERROR, STATUS_CODE_300_ERROR.getBytes())
                                  .build();
        replyMessageSupplier = () -> reply;
        var response = processRequest();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(300);
    }

    @Test
    void shouldReturnInternalServerErrorForBadErrorHeaderValue() {
        var reply = MessageBuilder.withPayload("")
                                  .setHeader(Headers.ERROR, "An value that won't parse".getBytes())
                                  .build();
        replyMessageSupplier = () -> reply;
        var response = processRequest();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void shouldReturnGatewayTimeOutForMessageReplyTimeout() {
        when(messageHandler.sendAndReceive(any(), any())).thenReturn(CompletableFuture.failedFuture(new KafkaReplyTimeoutException("Timeout")));
        var response = processRequest();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
    }

    @Test
    void shouldHandleRuntimeExceptionThrownByMessageHandler() {
        when(messageHandler.sendAndReceive(any(), any())).thenThrow(new RuntimeException());
        var response = processRequest();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void shouldReturnMapOfValuesOfMessagePayload() {
        replyMessageSupplier = () -> MessageBuilder.withPayload("""
                                                        {
                                                            "a" : "b"
                                                        }
                                                       """)
                                                   .build();
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
