package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.ResolvableType;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RpcMethod_Test {

    public static final String METHOD_NAME = "MethodX";

    private JacksonTester<RpcMethod> tester;

    private RpcMethod rpcMethod = new RpcMethod(METHOD_NAME);

    @BeforeEach
    void setUp() {
        this.tester = new JacksonTester<>(RpcMethod_Test.class,
                                          ResolvableType.forClass(RpcMethod.class),
                                          new ObjectMapper());
    }

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