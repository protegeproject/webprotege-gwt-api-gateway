package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties
@ConfigurationPropertiesScan
@SpringBootApplication
public class WebprotegeGwtApiGatewayApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(WebprotegeGwtApiGatewayApplication.class);

	@Value("${webprotege.apigateway.forceUserName:}")
	private String forceUserName;


	public static void main(String[] args) {
		SpringApplication.run(WebprotegeGwtApiGatewayApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if(!forceUserName.isEmpty()) {
			logger.warn("Forcing user name {} for every request", forceUserName);
		}
	}

	@Bean
	RpcRequestProcessor rpcRequestProcessor(ObjectMapper objectMapper,
											Messenger messenger,
											RabbitTemplate rabbitTemplate,
											TopicExchange topicExchange) {
		return new RpcRequestProcessor(messenger, objectMapper, rabbitTemplate, topicExchange);
	}

	@Bean
	MessengerImpl messageHandler(RabbitTemplate rabbitTemplate) {
		return new MessengerImpl(rabbitTemplate);
	}
}
