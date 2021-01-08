package jt.skunkworks.dataflow.config;

import jt.skunkworks.dataflow.Main;
import jt.skunkworks.dataflow.mock.MockMessageBuilder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.matchers.Times;
import org.mockserver.model.Delay;
import org.mockserver.model.RequestDefinition;
import org.mockserver.verify.VerificationTimes;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static jt.skunkworks.dataflow.util.TestUtil.mockFundSufficientCheck;
import static jt.skunkworks.dataflow.util.TestUtil.mockFundSufficientCheckResponse;
import static org.springframework.amqp.core.MessageBuilder.withBody;

@Tag("integration")
@SpringBootTest(classes = {Main.class})
@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {8091})
public class ErrorHandlingTest {

    private final MockMessageBuilder builder = new MockMessageBuilder();

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @AfterEach
    void tearDown(MockServerClient client) {
        client.reset();
    }

    @Test
    @SneakyThrows
    void socketTimeoutTest(MockServerClient client) {
        RequestDefinition requestDefinition = mockFundSufficientCheck();
        client.when(mockFundSufficientCheck(), Times.exactly(4))
                .respond(mockFundSufficientCheckResponse(200, "APPROVED")
                        .withDelay(Delay.milliseconds(2500)));

        rabbitTemplate.send("skunkworks",
                "skunkworks:fx.order",
                withBody(builder.build().getBytes())
                        .setHeader("order-id", builder.getOrderId())
                        .build());

        Thread.sleep(10000L);
        client.verify(requestDefinition, VerificationTimes.exactly(3));
        // MaxAttempt = 3 , 1st attempt + 2 retry attempt
    }

}
