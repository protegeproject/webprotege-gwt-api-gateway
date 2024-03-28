package edu.stanford.protege.webprotege.gateway;

import com.rabbitmq.client.ConnectionFactory;
import edu.stanford.protege.webprotege.ipc.WebProtegeIpcApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(classes = WebprotegeGwtApiGatewayApplication.class)
@Import({MockJwtDecoderConfiguration.class})
@ExtendWith(IntegrationTestsExtension.class)
class WebprotegeGwtApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}
}
