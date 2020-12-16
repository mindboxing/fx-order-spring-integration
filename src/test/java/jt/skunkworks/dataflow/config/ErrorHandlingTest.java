package jt.skunkworks.dataflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jt.skunkworks.dataflow.Main;
import jt.skunkworks.dataflow.message.FxOrder;
import jt.skunkworks.dataflow.service.FxOrderTransformer;
import jt.skunkworks.dataflow.service.client.SufficientFundCheckRequest;
import jt.skunkworks.dataflow.service.client.SufficientFundCheckRequestBuilder;
import jt.skunkworks.fx.Order;
import jt.skunkworks.dataflow.mock.FxMessageBuilder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.matchers.Times;
import org.mockserver.model.Delay;
import org.mockserver.model.MediaType;
import org.mockserver.model.RequestDefinition;
import org.mockserver.verify.VerificationTimes;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.amqp.core.MessageBuilder.withBody;

@SpringBootTest(classes = {Main.class})
@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {8091})
public class ErrorHandlingTest {

    private final FxMessageBuilder builder = new FxMessageBuilder();

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper om;

    private SufficientFundCheckRequest request;

    @BeforeEach
    @SneakyThrows
    void setup() {
        String accountId = "00000000-0000-0000-0000-000000000000";
        Order order = builder.accountId(accountId).buildOrder();
        FxOrder fxOrder = new FxOrderTransformer().transform(order);
        request = new SufficientFundCheckRequestBuilder(fxOrder).build();
    }


    @Test
    @SneakyThrows
    void socketTimeoutTest(MockServerClient client) {
        RequestDefinition requestDefinition = mock(request);
        client.when(requestDefinition, Times.exactly(4))
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"result\": \"APPROVED\"}")
                        .withDelay(Delay.milliseconds(2500)));

        var message = builder.build();

        rabbitTemplate.send("skunkworks",
                "skunkworks:fx.order",
                withBody(message.getBytes())
                        .setHeader("order-id", builder.getOrderId())
                        .build());

        Thread.sleep(10000L);
        client.verify(requestDefinition, VerificationTimes.exactly(3));
        // MaxAttempt = 3 , 1st attempt + 2 retry attempt
    }

    @SneakyThrows
    private RequestDefinition mock(SufficientFundCheckRequest request) {
        String json = om.writeValueAsString(request);
        return request()
                .withMethod("POST")
                .withPath("/fund/00000000-0000-0000-0000-000000000000/fundSufficientCheck")
                .withBody(json);
    }
}
