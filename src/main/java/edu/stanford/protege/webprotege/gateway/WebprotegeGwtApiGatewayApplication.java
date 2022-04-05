package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.protege.webprotege.ipc.pulsar.PulsarProducersManager;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@EnableConfigurationProperties
@ConfigurationPropertiesScan
@SpringBootApplication
public class WebprotegeGwtApiGatewayApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(WebprotegeGwtApiGatewayApplication.class);

	@Value("${webprotege.gateway.reply-channel}")
	private String replyTopic;

	@Value("${webprotege.pulsar.serviceUrl}")
	private String pulsarServiceUrl;

	@Value("${spring.application.name}")
	private String applicationName;

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
	PulsarClient pulsarClient() throws PulsarClientException {
		return PulsarClient.builder()
						   .serviceUrl(pulsarServiceUrl).build();
	}

	@Bean
	PulsarProducersManager producersManager(PulsarClient pulsarClient) {
		return new PulsarProducersManager(pulsarClient, applicationName);
	}

	@Bean
	RpcRequestProcessor rpcRequestProcessor(ObjectMapper objectMapper,
											Messenger messenger) {
		return new RpcRequestProcessor(messenger, objectMapper);
	}

	@Bean
	MessengerPulsarImpl messageHandler(PulsarClient pulsarClient,
									   PulsarProducersManager producersManager,
									   ObjectMapper objectMapper) {
		return new MessengerPulsarImpl(pulsarClient, producersManager, objectMapper);
	}
}
