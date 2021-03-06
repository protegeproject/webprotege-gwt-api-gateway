package edu.stanford.protege.webprotege.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Import;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SpringBootTest(classes = WebprotegeGwtApiGatewayApplication.class)
@Import(MockJwtDecoderConfiguration.class)
@EnableConfigurationProperties(value = GatewayProperties.class)
@AutoConfigureJsonTesters
class RpcMethod_Test {

    public static final String METHOD_NAME = "MethodX";

    @Autowired
    private JacksonTester<RpcMethod> tester;

    private RpcMethod rpcMethod = new RpcMethod(METHOD_NAME);

    @Test
    void shouldSerializeRpcMethod() throws IOException {
        var written = tester.write(rpcMethod);
        var json = written.getJson();
        assertThat(json, is("\"MethodX\""));
    }

    @Test
    void shouldDesearializeRpcMethod() throws IOException {
        var parsed = tester.parse("\"MethodX\"");
        assertThat(parsed.getObject().getMethodName(), is(METHOD_NAME));
    }
}