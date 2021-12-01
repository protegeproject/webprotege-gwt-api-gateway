package edu.stanford.protege.webprotege.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@EnableConfigurationProperties
@ConfigurationPropertiesScan
@SpringBootApplication
public class WebprotegeGwtApiGatewayApplication {

	@Value("${webprotege.gateway.reply-channel}")
	private String replyTopic;

	@Value("${spring.application.name}")
	private String groupId;

	@Value("${spring.kafka.producer.bootstrap-servers}")
	private String bootstrapServers;

	public static void main(String[] args) {
		SpringApplication.run(WebprotegeGwtApiGatewayApplication.class, args);
	}

	@Bean
	ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate(ProducerFactory<String, String> producerFactory,
																		ConcurrentMessageListenerContainer<String, String> messageContainer) {
		var template = new ReplyingKafkaTemplate<>(producerFactory, messageContainer);
		template.setDefaultReplyTimeout(Duration.ofMinutes(1));
		return template;
	}

	@Bean
	ConcurrentMessageListenerContainer<String, String> concurrentMessageListenerContainer(ConsumerFactory<? super String, ? super String> consumerFactory,
																						  ContainerProperties containerProperties) {
		return new ConcurrentMessageListenerContainer<>(consumerFactory,
														containerProperties);
	}

	@Bean
	ContainerProperties consumerProperties() {
		return new ContainerProperties(replyTopic);
	}

	@Bean
	ConcurrentKafkaListenerContainerFactory<String, String> containerFactory() {
		return new ConcurrentKafkaListenerContainerFactory<>();
	}

	@Bean
	ProducerFactory<String, String> producerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer());
	}

	@Bean
	RpcRequestProcessor messageProcessor(ObjectMapper objectMapper, MessageHandler messageHandler) {
		return new RpcRequestProcessor(messageHandler, objectMapper, replyTopic, Duration.ofSeconds(60));
	}

	@Bean
	MessageHandler messageHandler(ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate) {
		return new MessageHandler(replyingKafkaTemplate);
	}
}
