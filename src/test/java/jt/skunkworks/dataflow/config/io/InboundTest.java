package jt.skunkworks.dataflow.config.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = InboundTest.Config.class)
public class InboundTest {

    @Autowired
    MessageChannel input;

    @Autowired
    QueueChannel testOutput;

    @Test
    void test() {
        input.send(MessageBuilder.withPayload("TEST").build());
        Message<?> receive = testOutput.receive(0);
        Assertions.assertNotNull(receive);
    }


    @Configuration
    static class Config {
        @Bean
        public IntegrationFlow inboundFlow() {
            return IntegrationFlows.from("input")
                    .channel("output")
                    .get();
        }

        @Bean
        IntegrationFlow bridgeOutput() {
            return IntegrationFlows
                    .from("output")
                    .channel("testOutput")
                    .get();
        }

        @Bean
        QueueChannel testOutput() {
            return MessageChannels.queue("testOutput")
                    .get();
        }
    }
}
