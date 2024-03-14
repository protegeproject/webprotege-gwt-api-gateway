package edu.stanford.protege.webprotege.gateway;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitClientConfiguration {

    private final static Logger logger = LoggerFactory.getLogger(RabbitClientConfiguration.class);

    public static final String RPC_QUEUE1 = "webprotege-api-gateway-queue";
    public static final String RPC_RESPONSE_QUEUE = "webprotege-api-gateway-response-queue";

    public static final String RPC_EXCHANGE = "webprotege-exchange";

    @Value("${spring.rabbitmq.host}")
    private String rabbitMqHost;

    @Bean
    Queue msgQueue() {
        return new Queue(RPC_QUEUE1, true);
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
        response.setHost(rabbitMqHost);
        return response;
    }


    @Bean
    Binding msgBinding() {
        com.rabbitmq.client.ConnectionFactory connectionFactory = connectionFactory();
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(RPC_EXCHANGE, "direct", true);
            channel.queueDeclare(RPC_QUEUE1,true,false, false,null);
            channel.queueDeclare(RPC_RESPONSE_QUEUE,true,false, false,null);

            channel.queueBind(RPC_QUEUE1, RPC_EXCHANGE, "webprotege.api-gateway-response.queue");
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return BindingBuilder.bind(msgQueue()).to(exchange()).with(RPC_QUEUE1);
    }

    @Bean
    Binding replyBinding() {
        return BindingBuilder.bind(replyQueue()).to(exchange()).with(RPC_RESPONSE_QUEUE);
    }



    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setReplyAddress(RPC_RESPONSE_QUEUE);
        template.setReplyTimeout(6000);
        return template;
    }


    @Bean
    SimpleMessageListenerContainer replyContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RPC_RESPONSE_QUEUE);
        container.setMessageListener(rabbitTemplate(connectionFactory));
        return container;
    }

}
