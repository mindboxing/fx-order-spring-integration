package jt.skunkworks.dataflow.config;

import jt.skunkworks.dataflow.message.event.Event;
import jt.skunkworks.dataflow.util.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.RequestDefinition;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import static jt.skunkworks.dataflow.util.TestUtil.mockFundSufficientCheck;
import static jt.skunkworks.dataflow.util.TestUtil.mockFundSufficientCheckResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockserver.model.HttpResponse.response;

@SpringJUnitConfig
@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {8091})
public class MessageProcessTest {

    @Autowired
    @Qualifier("process.input")
    private MessageChannel input;

    @Autowired
    @Qualifier("test.output")
    private QueueChannel output;

    @Autowired
    @Qualifier("test.error")
    private QueueChannel error;

    @Autowired
    private ApplicationContext context;

    @AfterEach
    void tearDown(MockServerClient client) {
        client.reset();
    }

    @Test
    @DisplayName("Happy path ACCEPTED")
    void testAccepted(MockServerClient client) {
        client.when(mockFundSufficientCheck())
                .respond(mockFundSufficientCheckResponse(200, "APPROVED"));

        input.send(MessageBuilder.withPayload(TestUtil.testPayload()).build());

        Message<?> receive = output.receive(500L);
        assertNotNull(receive);
        Event event = (Event) receive.getPayload();
        assertEquals(Event.EventType.ORDER_ACCEPTED, event.getType());
    }

    @Test
    @DisplayName("Happy path REJECTED")
    void testRejected(MockServerClient client) {
        client.when(mockFundSufficientCheck())
                .respond(mockFundSufficientCheckResponse(200, "INSUFFICIENT"));

        input.send(MessageBuilder.withPayload(TestUtil.testPayload()).build());

        Message<?> receive = output.receive(500L);
        assertNotNull(receive);
        Event event = (Event) receive.getPayload();
        assertEquals(Event.EventType.ORDER_REJECTED, event.getType());
    }

    @Test
    @DisplayName("REST call fundSufficientCheck result in 400")
    void fundSufficientCheckBadRequest(MockServerClient client) {
        RequestDefinition request = mockFundSufficientCheck();
        client.when(request).respond(response().withStatusCode(400).withBody(""));

        input.send(MessageBuilder.withPayload(TestUtil.testPayload()).build());

        Message<?> receive = output.receive(2000L);
        Assertions.assertNull(receive);
        client.verify(request, VerificationTimes.once());
        Message<?> errorMsg = error.receive(1000L);
        assertNotNull(errorMsg);
        MessagingException errorMsgPayload = (MessagingException) errorMsg.getPayload();
        HttpClientErrorException cause = (HttpClientErrorException) errorMsgPayload.getCause().getCause();
        assertEquals(400, cause.getRawStatusCode());
    }

    @Test
    @DisplayName("REST call fundSufficientCheck result in 500")
    void fundSufficientCheckInternalServerError(MockServerClient client) {
        RequestDefinition request = mockFundSufficientCheck();
        client.when(request).respond(response().withStatusCode(500).withBody(""));

        input.send(MessageBuilder.withPayload(TestUtil.testPayload()).build());

        Message<?> receive = output.receive(500L);
        Assertions.assertNull(receive);
        client.verify(request, VerificationTimes.once());
        Message<?> errorMsg = error.receive(500L);
        assertNotNull(errorMsg);
        MessagingException errorMsgPayload = (MessagingException) errorMsg.getPayload();
        HttpServerErrorException cause = (HttpServerErrorException) errorMsgPayload.getCause().getCause();
        assertEquals(500, cause.getRawStatusCode());
    }

    @Test
    @DisplayName("REST call fundSufficientCheck result in SocketTimeoutException")
    void fundSufficientCheckInternalRetry(MockServerClient client) {
        RequestDefinition request = mockFundSufficientCheck();
        client.when(request)
                .respond(
                        mockFundSufficientCheckResponse(200, "INSUFFICIENT")
                                .withDelay(TimeUnit.MILLISECONDS, 2700)
                );

        input.send(MessageBuilder.withPayload(TestUtil.testPayload()).build());

        Message<?> receive = output.receive(11000L);
        client.verify(request, VerificationTimes.exactly(3));
        Message<?> errorMsg = error.receive(500L);
        assertNotNull(errorMsg);
        MessagingException errorMsgPayload = (MessagingException) errorMsg.getPayload();
        SocketTimeoutException cause = (SocketTimeoutException) errorMsgPayload.getCause().getCause().getCause();
        assertEquals("Read timed out", cause.getMessage());
    }


    @Configuration
    @EnableIntegration
    @Import({MessageFlowConfig.class, MiscConfig.class})
    static class Config {

        @Bean("process.input")
        public MessageChannel processInputChanel() {
            return MessageChannels.publishSubscribe().get();
        }

        @Bean("test.output")
        public QueueChannel testOutputChannel() {
            return MessageChannels.queue().get();
        }

        @Bean("test.error")
        public QueueChannel testErrorhannel() {
            return MessageChannels.queue().get();
        }

        @Bean
        public IntegrationFlow testOutbound() {
            return IntegrationFlows
                    .from("process.output")
                    .transform(new JsonToObjectTransformer(Event.class))
                    .channel("test.output")
                    .get();
        }

        @Bean
        public IntegrationFlow testDeadLetter() {
            return IntegrationFlows
                    .from("errorChannel")
                    .channel("test.error")
                    .get();
        }
    }
}
