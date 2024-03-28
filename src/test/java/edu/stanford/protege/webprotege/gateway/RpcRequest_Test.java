package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.ResolvableType;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RpcRequest_Test {

    public static final String METHOD_NAME = "TheMethod";

    private JacksonTester<RpcRequest> tester;

    @BeforeEach
    void setUp() {
        tester = new JacksonTester<>(RpcRequest_Test.class,
                                     ResolvableType.forClass(RpcRequest.class),
                                     new ObjectMapper());
    }

    @Test
    void shouldSerializeRpcRequestWithoutParameters() throws IOException {
        var rpcRequest = new RpcRequest(new RpcMethod(METHOD_NAME), null);
        var json = tester.write(rpcRequest);
        assertThat(json).hasJsonPath("$.method", METHOD_NAME);
        assertThat(json).hasEmptyJsonPathValue("$.params");
    }

    @Test
    void shouldSerializeRpcRequestWithParameters() throws IOException {
        var parameters = new ObjectNode(new JsonNodeFactory(false));
        parameters.put("x", "y");
        var rpcRequest = new RpcRequest(new RpcMethod(METHOD_NAME), parameters);
        var json = tester.write(rpcRequest);
        assertThat(json).hasJsonPath("$.method", METHOD_NAME);
        assertThat(json).hasJsonPath("$.params.x", "y");
    }

    @Test
    void shouldDeserializeRpcRequestWithoutParameters() throws IOException {
        var json = """
                {"method":"TheMethod"}
                """;
        var parsed = tester.parse(json);
        assertThat(parsed.getObject().method().getMethodName()).isEqualTo(METHOD_NAME);
    }

    @Test
    void shouldDeserializeRpcRequestWithParameters() throws IOException {
        var json = """
                {"method":"TheMethod","params":{"x":"y"}}
                """;
        var parsed = tester.parse(json);
        assertThat(parsed.getObject().methodName()).isEqualTo(METHOD_NAME);
        var parameters = parsed.getObject().params();
        assertThat(parameters.get("x").asText()).isEqualTo("y");
    }
}