package edu.stanford.protege.webprotege.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = WebprotegeGwtApiGatewayApplication.class)
@Import(MockJwtDecoderConfiguration.class)
@ExtendWith(IntegrationTestsExtension.class)
class WebprotegeGwtApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}
}
