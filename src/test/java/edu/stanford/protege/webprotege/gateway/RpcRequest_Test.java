package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JsonTest
class RpcRequest_Test {

    public static final String METHOD_NAME = "TheMethod";

    /** @noinspection SpringJavaInjectionPointsAutowiringInspection*/
    @Autowired
    private JacksonTester<RpcRequest> tester;


    @Test
    void shouldSerializeRpcRequestWithoutParameters() throws IOException {
        var rpcRequest = new RpcRequest(new RpcMethod(METHOD_NAME), null);
        var json = tester.write(rpcRequest);
        assertThat(json).hasJsonPath("$.method", METHOD_NAME);
        assertThat(json).hasEmptyJsonPathValue("$.parameters");
    }

    @Test
    void shouldSerializeRpcRequestWithParameters() throws IOException {
        var parameters = new ObjectNode(new JsonNodeFactory(false));
        parameters.put("x", "y");
        var rpcRequest = new RpcRequest(new RpcMethod(METHOD_NAME), parameters);
        var json = tester.write(rpcRequest);
        assertThat(json).hasJsonPath("$.method", METHOD_NAME);
        assertThat(json).hasJsonPath("$.parameters.x", "y");
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
                {"method":"TheMethod","parameters":{"x":"y"}}
                """;
        var parsed = tester.parse(json);
        assertThat(parsed.getObject().methodName()).isEqualTo(METHOD_NAME);
        var parameters = parsed.getObject().parameters();
        assertThat(parameters.get("x").asText()).isEqualTo("y");
    }
}