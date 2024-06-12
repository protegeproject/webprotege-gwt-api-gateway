package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.ipc.WebProtegeIpcApplication;
import io.minio.MinioClient;
import org.slf4j.*;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;

@EnableConfigurationProperties
@ConfigurationPropertiesScan
@SpringBootApplication
@Import(WebProtegeIpcApplication.class)
public class WebprotegeGwtApiGatewayApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(WebprotegeGwtApiGatewayApplication.class);

	@Value("${webprotege.apigateway.forceUserName:}")
	private String forceUserName;


	public static void main(String[] args) {
		SpringApplication.run(WebprotegeGwtApiGatewayApplication.class, args);
	}

	@Override
	public void run(String... args) {
		if(!forceUserName.isEmpty()) {
			logger.warn("Forcing user name {} for every request", forceUserName);
		}
	}


	@Bean
	MinioClient minioClient(MinioProperties properties) {
		return MinioClient.builder()
								   .credentials(properties.getAccessKey(), properties.getSecretKey())
								   .endpoint(properties.getEndPoint())
								   .build();
	}

	@Bean
	@Lazy
	RpcRequestProcessor rpcRequestProcessor(ObjectMapper objectMapper,
											Messenger messenger) {
		return new RpcRequestProcessor(messenger, objectMapper);
	}

	@Bean
	@Lazy
	MessengerImpl messageHandler(AsyncRabbitTemplate rabbitTemplate) {
		return new MessengerImpl(rabbitTemplate);
	}
}
