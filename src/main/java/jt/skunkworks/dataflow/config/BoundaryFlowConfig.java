package jt.skunkworks.dataflow.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.dsl.MessageProducerSpec;

import static org.springframework.integration.handler.LoggingHandler.Level.ERROR;

@Configuration
public class BoundaryFlowConfig {

    @Bean
    public IntegrationFlow inboundFlow(
            @Qualifier("amqpInboundAdapter") MessageProducerSpec<?, ?> amqpInboundAdapter
    ) {
        return IntegrationFlows.from(amqpInboundAdapter)
                .channel("persistChannel")
                .channel("process.input")
                .get();
    }

    @Bean
    public IntegrationFlow outboundFlow(
            @Qualifier("amqpOutboundAdapter") MessageHandlerSpec<?, ?> amqpOutboundAdapter
    ) {
        return IntegrationFlows.from("process.output")
                .handle(amqpOutboundAdapter)
                .get();
    }

    @Bean
    public IntegrationFlow errorChannelFlow(@Qualifier("amqpDlqAdapter") MessageHandlerSpec<?, ?> amqpDlqAdapter) {
        return IntegrationFlows
                .from("errorChannel")
                .log(ERROR, "APP-ERROR")
                .handle(amqpDlqAdapter)
                .get();
    }

}
