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
import org.mockserver.model.HttpRequest;
import org.mockserver.model.MediaType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.amqp.core.MessageBuilder.withBody;

@SpringBootTest(classes = {Main.class})
@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {8091})
public class MessageFlowConfigTest {

    private FxMessageBuilder builder = new FxMessageBuilder();

    private final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private HttpRequest requestDefinition;

    @BeforeEach
    @SneakyThrows
    void setup() {
        String accountId = "00000000-0000-0000-0000-000000000000";
        Order order = builder.accountId(accountId).buildOrder();
        FxOrder fxOrder = new FxOrderTransformer().transform(order);
        SufficientFundCheckRequest request = new SufficientFundCheckRequestBuilder(fxOrder).build();
        String json = om.writeValueAsString(request);

        requestDefinition = request()
                .withMethod("POST")
                .withPath("/fund/" + accountId + "/fundSufficientCheck")
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(json);
    }


    @Test
    @SneakyThrows
    void test(MockServerClient client) {
        client.when(requestDefinition)
                .respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody("{\"result\": \"APPROVED\"}")
                );

        var message = builder.build();
        rabbitTemplate.send("skunkworks",
                "skunkworks:fx.order",
                withBody(message.getBytes())
                        .setHeader("order-id", builder.getOrderId())
                        .build());

        Thread.sleep(2000L);
    }

//    @Test
//    @SneakyThrows
//    void xmlTransformer() {
//        Jaxb2Marshaller jaxb = new Jaxb2Marshaller();
//        jaxb.setContextPath("jt.skunkworks.fx");
//
//        var message = builder.buildOrder();
//        var writer = new StringWriter();
//        jaxb.createMarshaller().marshal(message, writer);
//
//        var payload = writer.toString();
//        System.out.println(payload);
//
//        Object unmarshal = jaxb.createUnmarshaller().unmarshal(new StringSource(payload));
//        System.out.println(unmarshal);
//    }
}
