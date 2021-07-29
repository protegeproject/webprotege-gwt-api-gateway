package edu.stanford.protege.webprotege.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JsonTest
class RpcResponse_Test {

    @Autowired
    private JacksonTester<RpcResponse> tester;

    private final String correlationId = UUID.randomUUID().toString();

    @Test
    void shouldSerializeResponseContainingResult() throws IOException {
        var correlationId = UUID.randomUUID().toString();
        var result = new HashMap<String, Object>();
        result.put("x", "y");
        var response = new RpcResponse(correlationId,
                                       null,
                                       result);
        var json = tester.write(response);
        assertThat(json).hasJsonPath("$.correlationId", correlationId);
        assertThat(json).hasJsonPath("$.result.x", "y");
        assertThat(json).hasEmptyJsonPathValue("$.error");
    }

    @Test
    void shouldSerializeResponseContainingErrorCode() throws IOException {
        var errorData = new HashMap<String, Object>();
        errorData.put("x", "y");
        var errorMessage = "Error message";
        var code = 33;
        var error = new RpcError(code, errorMessage, errorData);
        var response = new RpcResponse(correlationId,
                                       error,
                                       null);
        var json = tester.write(response);
        assertThat(json).hasJsonPath("$.correlationId", correlationId);
        assertThat(json).hasJsonPath("$.error.code", code);
        assertThat(json).hasJsonPath("$.error.message", errorMessage);
        assertThat(json).hasJsonPath("$.error.data.x", "y");
        assertThat(json).hasEmptyJsonPathValue("$.result");
    }

    @Test
    void shouldNotAllowResultAndError() {
        assertThrows(IllegalArgumentException.class, () -> {
           new RpcResponse(correlationId, new RpcError(33, "", emptyMap()), emptyMap());
        });
    }

    @Test
    void shouldNotAllowNullResultAndNullError() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RpcResponse(correlationId, null, null);
        });
    }

    @Test
    void shouldNotAllowNullCorrelationId() {
        assertThrows(NullPointerException.class, () -> {
           new RpcResponse(null, null, emptyMap());
        });
    }
}