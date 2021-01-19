package jt.skunkworks.dataflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jt.skunkworks.dataflow.Main;
import jt.skunkworks.dataflow.message.event.Event;
import jt.skunkworks.dataflow.mock.MockMessageBuilder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.messaging.Message;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static jt.skunkworks.dataflow.mock.MockMessageBuilder.ACCOUNT_ID;
import static jt.skunkworks.dataflow.util.TestUtil.*;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.amqp.core.MessageBuilder.withBody;

@Tag("integration")
@SpringBootTest(classes = {Main.class, IntegrationTest.Config.class})
@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {8091})
public class IntegrationTest {

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    @Qualifier("integration.fxEvent")
    private QueueChannel result;

    @Test
    @SneakyThrows
    void test(MockServerClient client) {
        client.when(mockFundSufficientCheck())
                .respond(mockFundSufficientCheckResponse(200, "APPROVED"));

        MockMessageBuilder builder = new MockMessageBuilder().accountId(ACCOUNT_ID);

        rabbitTemplate.send("skunkworks", "skunkworks:fx.order",
                withBody(builder.build().getBytes()).build());


        Message<?> receive = result.receive(2000L);
        Assertions.assertNotNull(receive);
        Event payload = (Event) receive.getPayload();
        assertEquals(builder.getOrderId(), payload.getAggregateId());
        assertEquals(Event.EventType.ORDER_ACCEPTED, payload.getType());
    }

    @Configuration
    static class Config {
        @Bean
        public IntegrationFlow resultFlow(
                ConnectionFactory connectionFactory,
                Queue fxEvent
                ) {
            return IntegrationFlows
                    .from(Amqp.inboundAdapter(connectionFactory, fxEvent))
                    .transform(new JsonToObjectTransformer(Event.class))
                    .channel(MessageChannels.queue("integration.fxEvent"))
                    .get();
        }

    }

}
