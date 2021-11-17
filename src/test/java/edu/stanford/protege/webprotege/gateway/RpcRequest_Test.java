package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureJsonTesters
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