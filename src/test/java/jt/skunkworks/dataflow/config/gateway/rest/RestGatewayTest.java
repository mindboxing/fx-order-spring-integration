package jt.skunkworks.dataflow.config.gateway.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jt.skunkworks.dataflow.message.SufficientFundCheckRequest;
import jt.skunkworks.dataflow.message.SufficientFundCheckRequestBuilder;
import jt.skunkworks.dataflow.message.SufficientFundCheckResponse;
import jt.skunkworks.dataflow.mock.MockMessageBuilder;
import jt.skunkworks.fx.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.RequestDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.*;
import org.springframework.integration.http.dsl.Http;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestTemplate;

import java.util.stream.Stream;

import static jt.skunkworks.dataflow.util.TestUtil.mockFundSufficientCheck;
import static jt.skunkworks.dataflow.util.TestUtil.mockFundSufficientCheckResponse;
import static org.mockserver.model.HttpResponse.response;

@SpringJUnitConfig(classes = RestGatewayTest.Config.class)
@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {8091})
public class RestGatewayTest {

    @Autowired
    private MessageChannel input;
    @Autowired
    private QueueChannel output;
    private MockMessageBuilder messageBuilder = new MockMessageBuilder();

    @Autowired
    ApplicationContext context;

    @AfterEach
    void tearDown(MockServerClient client) {
        client.reset();
    }

    @Test
    void testApproved(MockServerClient client) {
        // GIVEN
        Order order = messageBuilder.buildOrder();
        SufficientFundCheckRequest request = new SufficientFundCheckRequestBuilder(order).build();
        RequestDefinition requestDefinition = mockFundSufficientCheck(request, order.getAccountId().toString());
        client.when(requestDefinition)
                .respond(mockFundSufficientCheckResponse(200, "APPROVED"));

        // WHEN
        input.send(MessageBuilder
                .withPayload(request)
                .setHeader("accountId", order.getAccountId())
                .build());

        // THEN
        Message<?> receive = output.receive(2000l);
        Assertions.assertNotNull(receive);
        SufficientFundCheckResponse payload = (SufficientFundCheckResponse) receive.getPayload();
        System.out.println(payload);
    }

    @Test
    void testInsufficient(MockServerClient client) {
        // GIVEN
        Order order = messageBuilder.buildOrder();
        SufficientFundCheckRequest request = new SufficientFundCheckRequestBuilder(order).build();
        RequestDefinition requestDefinition = mockFundSufficientCheck(request, order.getAccountId().toString());
        client.when(requestDefinition)
                .respond(mockFundSufficientCheckResponse(200, "INSUFFICIENT"));

        // WHEN
        input.send(MessageBuilder
                .withPayload(request)
                .setHeader("accountId", order.getAccountId())
                .build());

        // THEN
        Message<?> receive = output.receive(2000L);
        Assertions.assertNotNull(receive);
        SufficientFundCheckResponse payload = (SufficientFundCheckResponse) receive.getPayload();
        System.out.println(payload);
    }

    @Test
    void testBadRequest(MockServerClient client) {
        // GIVEN
        Order order = messageBuilder.buildOrder();
        SufficientFundCheckRequest request = new SufficientFundCheckRequestBuilder(order).build();
        RequestDefinition requestDefinition = mockFundSufficientCheck(request, order.getAccountId().toString());
        client.when(requestDefinition)
                .respond(response().withStatusCode(400));

        // WHEN
        input.send(MessageBuilder
                .withPayload(request)
                .setHeader("accountId", order.getAccountId())
                .build());

        // THEN
        Message<?> receive = output.receive(2000L);
        Assertions.assertNull(receive);
    }

    @Test
    void printContext() {
        Stream.of(context.getBeanDefinitionNames()).forEach(System.out::println);
    }

    @Configuration
    @IntegrationComponentScan
    @EnableIntegration
    static class Config {

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .registerModule(new JavaTimeModule());
        }

        @Bean(name = PollerMetadata.DEFAULT_POLLER)
        public PollerSpec poller() {
            return Pollers.fixedRate(500)
                    .errorChannel("errorChannel");
        }

        @Bean
        public IntegrationFlow fundSufficientCheckFlow() {
            RestTemplate restTemplate = new RestTemplate();
            return flow ->
                    flow
                    .transform(new ObjectToJsonTransformer())
                    // TODO - wireTap to log input
                    .handle(Http
                            .outboundGateway("http://localhost:8091/fund/{accountId}/fundSufficientCheck", restTemplate)
                            .expectedResponseType(SufficientFundCheckResponse.class)
                            .uriVariable("accountId", "headers['accountId']")
                            // TODO - OR interceptor to log message
                    )
                    // TODO - wireTap to log output
                    ;
        }

        @Bean
        public IntegrationFlow testFlow() {
            return IntegrationFlows.from(MessageChannels.queue("input"))
                    .gateway("fundSufficientCheckFlow.input")
                    .channel(MessageChannels.queue("output"))
                    .get();
        }

            @Bean
        public IntegrationFlow errorHandlingFlow() {
            return IntegrationFlows
                    .from("errorChannel")
                    .handle((payload, header) -> {
                        System.out.println("!!! Error");
                        System.out.println("!!! Error-Payload " + payload);
                        System.out.println("!!! Error-Header " + header);
                        return payload;
                    })
                    .get();
        }
    }
}

