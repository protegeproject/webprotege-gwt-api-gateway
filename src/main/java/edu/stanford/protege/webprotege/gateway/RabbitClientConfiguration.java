package edu.stanford.protege.webprotege.gateway;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitClientConfiguration {

    private final static Logger logger = LoggerFactory.getLogger(RabbitClientConfiguration.class);

    public static final String RPC_COMMAND_QUEUE = "webprotege-api-gateway-queue";
    public static final String RPC_RESPONSE_QUEUE = "webprotege-api-gateway-response-queue";

    public static final String RPC_EXCHANGE = "webprotege-exchange";

    @Bean
    Queue msgQueue() {
        return new Queue(RPC_COMMAND_QUEUE, true);
    }

    @Bean
    Queue replyQueue() {
        return new Queue(RPC_RESPONSE_QUEUE, true);
    }

    @Bean
    DirectExchange exchange() {
        return new DirectExchange(RPC_EXCHANGE, true ,false);
    }

    @Bean
    public com.rabbitmq.client.ConnectionFactory connectionFactory(){
        com.rabbitmq.client.ConnectionFactory response = new com.rabbitmq.client.ConnectionFactory();
        response.setHost("rabbitmq");
        return response;
    }


    @Bean
    Binding msgBinding() {
        com.rabbitmq.client.ConnectionFactory connectionFactory = connectionFactory();
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(RPC_EXCHANGE, "direct", true);
            channel.queueDeclare(RPC_COMMAND_QUEUE,true,false, false,null);
            channel.queueDeclare(RPC_RESPONSE_QUEUE,true,false, false,null);

            channel.queueBind(RPC_COMMAND_QUEUE, RPC_EXCHANGE, "webprotege.api-gateway-response.queue");
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return BindingBuilder.bind(msgQueue()).to(exchange()).with(RPC_COMMAND_QUEUE);
    }

    @Bean
    Binding replyBinding() {
        return BindingBuilder.bind(replyQueue()).to(exchange()).with(RPC_RESPONSE_QUEUE);
    }



    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setExchange(RPC_EXCHANGE);
        return rabbitTemplate;
    }

    @Bean
    AsyncRabbitTemplate asyncRabbitTemplate(RabbitTemplate rabbitTemplate,  SimpleMessageListenerContainer replyContainer) {
        return new AsyncRabbitTemplate(rabbitTemplate,replyContainer, RPC_RESPONSE_QUEUE);
    }

    @Bean
    SimpleMessageListenerContainer replyContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RPC_RESPONSE_QUEUE);
        return container;
    }

}
