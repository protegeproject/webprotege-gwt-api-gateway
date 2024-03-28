package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.ipc.WebProtegeIpcApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

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
