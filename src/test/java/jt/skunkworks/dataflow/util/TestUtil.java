package jt.skunkworks.dataflow.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jt.skunkworks.dataflow.message.SufficientFundCheckRequest;
import jt.skunkworks.dataflow.message.SufficientFundCheckRequestBuilder;
import jt.skunkworks.dataflow.mock.MockMessageBuilder;
import jt.skunkworks.fx.Order;
import lombok.SneakyThrows;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.mockserver.model.RequestDefinition;

import static jt.skunkworks.dataflow.mock.MockMessageBuilder.ACCOUNT_ID;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class TestUtil {

    public final static ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @SneakyThrows
    public static RequestDefinition mockFundSufficientCheck() {
        return mockFundSufficientCheck(ACCOUNT_ID);
    }

    @SneakyThrows
    public static RequestDefinition mockFundSufficientCheck(String accountId) {
        return mockFundSufficientCheck(buildSufficientFundCheckRequest(accountId), accountId);
    }

    @SneakyThrows
    public static RequestDefinition mockFundSufficientCheck(SufficientFundCheckRequest request, String accountId) {
        return request()
                .withMethod("POST")
                .withPath("/fund/" + accountId + "/fundSufficientCheck")
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(OBJECT_MAPPER.writeValueAsString(request));
    }

    @SneakyThrows
    public static HttpResponse mockFundSufficientCheckResponse(int statusCode, String result) {
        HttpResponse httpResponse = response()
                .withStatusCode(statusCode)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("{\"result\": \""+result+"\"}");
        return httpResponse;
    }

    public static SufficientFundCheckRequest buildSufficientFundCheckRequest(String accountId) {
        MockMessageBuilder builder = new MockMessageBuilder();
        Order order = builder.accountId(accountId).buildOrder();
        return new SufficientFundCheckRequestBuilder(order).build();
    }

    public static String testPayload() {
        MockMessageBuilder builder = new MockMessageBuilder().accountId(ACCOUNT_ID);
        return builder.build();
    }

}
