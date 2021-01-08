package jt.skunkworks.dataflow.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.amqp.dsl.AmqpInboundChannelAdapterSMLCSpec;
import org.springframework.integration.amqp.dsl.AmqpOutboundChannelAdapterSpec;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;

@Configuration
public class RabbitMqConfig {
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        return new CachingConnectionFactory("localhost");
    }

    @Bean
    public Queue fxOrder() {
        return QueueBuilder
                .durable("fx.order")
                .deadLetterExchange("skunkworks.dlx")
                .build();
    }

    @Bean
    public Queue fxOrderDlq() {
        return QueueBuilder
                .durable("fx.order.dlq")
                .build();
    }

    @Bean
    public Queue fxEvent() {
        return QueueBuilder
                .durable("fx.event")
                .build();
    }

    @Bean
    public FanoutExchange dlqExchange() {
        return new FanoutExchange("skunkworks.dlx");
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("skunkworks");
    }

    @Bean
    public Binding fxPOrderBinding() {
        return BindingBuilder
                .bind(fxOrder())
                .to(exchange())
                .with("skunkworks:fx.order");
    }

    @Bean
    public Binding fxEventBinding() {
        return BindingBuilder
                .bind(fxEvent())
                .to(exchange())
                .with("skunkworks:fx.event");
    }

    @Bean
    public Binding fxPOrderDlqBinding() {
        return BindingBuilder
                .bind(fxOrderDlq())
                .to(dlqExchange())
                ;
    }

    @Bean
    public AmqpInboundChannelAdapterSMLCSpec amqpInboundAdapter(
            ConnectionFactory connectionFactory,
            Queue fxOrder,
            IdempotentReceiverInterceptor idempotentReceiverInterceptor
    ) {
        return Amqp.inboundAdapter(connectionFactory, fxOrder).configureContainer(c -> c.adviceChain(idempotentReceiverInterceptor));
    }

    @Bean
    public AmqpOutboundChannelAdapterSpec amqpOutboundAdapter(
            AmqpTemplate amqpTemplate
    ) {
        return Amqp.outboundAdapter(amqpTemplate).routingKey("fx.event");
    }

    @Bean
    public AmqpOutboundChannelAdapterSpec amqpDlqAdapter(
            AmqpTemplate amqpTemplate
    ) {
        return Amqp.outboundAdapter(amqpTemplate).routingKey("fx.order.dlq");
    }

}
