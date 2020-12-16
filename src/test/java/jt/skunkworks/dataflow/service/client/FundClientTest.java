package jt.skunkworks.dataflow.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jt.skunkworks.dataflow.config.AppConfigProps;
import jt.skunkworks.dataflow.message.FxOrder;
import jt.skunkworks.dataflow.service.FxOrderTransformer;
import jt.skunkworks.fx.Order;
import jt.skunkworks.dataflow.mock.FxMessageBuilder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.Delay;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {8091})
class FundClientTest {

    private final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private HttpRequest requestDefinition;
    private FxOrder fxOrder;
    private FundClient fundClient;
    private final AppConfigProps configProps = new AppConfigProps();

    @BeforeEach
    @SneakyThrows
    void setup() {

        configProps.setFundServiceUrl("http://localhost:8091");
        fundClient = new FundClient(new RestTemplate(), configProps);
        String accountId = "00000000-0000-0000-0000-000000000000";
        Order order = new FxMessageBuilder().accountId(accountId).buildOrder();
        fxOrder = new FxOrderTransformer().transform(order);
        SufficientFundCheckRequest request = new SufficientFundCheckRequestBuilder(fxOrder).build();

        requestDefinition = request()
                .withMethod("POST")
                .withPath("/fund/" + accountId + "/fundSufficientCheck")
                .withBody(om.writeValueAsString(request));
    }

    @Test
    @SneakyThrows
    void testApproved(MockServerClient client) {
        client.when(requestDefinition)
                .respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody("{\"result\": \"APPROVED\"}")
                );

        boolean actual = fundClient.sufficientFundCheck(fxOrder);
        client.verify(requestDefinition);
        Assertions.assertTrue(actual);
    }

    @Test
    @SneakyThrows
    void testInsufficent(MockServerClient client) {
        client.when(requestDefinition)
                .respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody("{\"result\": \"INSUFFICIENT\"}")
                );

        boolean actual = fundClient.sufficientFundCheck(fxOrder);
        client.verify(requestDefinition);
        Assertions.assertFalse(actual);
    }

    @Test
    void testBadRequest(MockServerClient client) {
        client.when(requestDefinition)
                .respond(
                        response()
                                .withStatusCode(400)
                                .withBody("{\"messageCode\": \"FC-0001\"}")
                );
        Assertions.assertThrows(HttpClientErrorException.class, () -> fundClient.sufficientFundCheck(fxOrder));
        client.verify(requestDefinition);
    }

    @Test
    void testInternalServerError(MockServerClient client) {
        client.when(requestDefinition).respond(response().withStatusCode(500));
        Assertions.assertThrows(HttpServerErrorException.InternalServerError.class, () -> fundClient.sufficientFundCheck(fxOrder));
        client.verify(requestDefinition);
    }

    @Test
    void testSocketTimeout(MockServerClient client) {
        SimpleClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory();
        httpRequestFactory.setReadTimeout(2000);
        RestTemplate restTemplate = new RestTemplate(httpRequestFactory);

        //restTemplate
        fundClient = new FundClient(restTemplate, configProps);

        client.when(requestDefinition)
                .respond(
                        response()
                                .withDelay(Delay.milliseconds(2500))
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody("{\"result\": \"APPROVED\"}")
                );
        System.out.println("!!! - HERE");
        ResourceAccessException exception = Assertions.assertThrows(ResourceAccessException.class, () -> fundClient.sufficientFundCheck(fxOrder));
        Assertions.assertEquals(exception.getCause().getClass(), SocketTimeoutException.class);
    }

}