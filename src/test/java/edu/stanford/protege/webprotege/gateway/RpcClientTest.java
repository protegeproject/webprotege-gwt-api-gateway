package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RpcClientTest {

    private RpcClient client;

    @Mock
    private RpcRequestProcessor requestProcessor;

    @Mock
    ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        client = new RpcClient(requestProcessor,
                               objectMapper);
    }

    @Test
    void shouldReturn200OkResult() {
        // Given a normal completion
        var resultMap = Map.<String, Object>of("foo", "bar");
        var response = RpcResponse.forResult("theMethod", resultMap);
        var future = CompletableFuture.completedFuture(response);
        when(requestProcessor.processRequest(any(), any(), any()))
                .thenReturn(future);
        var result = client.call(mock(Jwt.class), mock(RpcMethod.class), Map.of());
        // Result is 200 OK
        assertThat(result.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(result.getBody()).isEqualTo(resultMap);
    }

    @Test
    void shouldThrowExceptionOnRpcErrorCode() {
        // Given an error completion
        var response = RpcResponse.forError("TheErrorMessage", HttpStatus.valueOf(400));
        var future = CompletableFuture.completedFuture(response);
        when(requestProcessor.processRequest(any(), any(), any()))
                .thenReturn(future);
        var thrown = assertThrowsExactly(ResponseStatusException.class, () -> {
            client.call(mock(Jwt.class), mock(RpcMethod.class), Map.of());
        });
        assertThat(thrown.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void shouldThrow500ErrorResultOnInternalErrorFromCompletion() {
        // Given an error completion
        when(requestProcessor.processRequest(any(), any(), any()))
                .thenThrow(new RuntimeException("Some internal error"));
        var thrown = assertThrowsExactly(ResponseStatusException.class, () -> {
            client.call(mock(Jwt.class), mock(RpcMethod.class), Map.of());
        });
        assertThat(thrown.getStatusCode().value()).isEqualTo(500);

    }
}