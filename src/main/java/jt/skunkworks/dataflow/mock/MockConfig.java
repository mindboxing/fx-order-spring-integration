package jt.skunkworks.dataflow.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import jt.skunkworks.dataflow.message.SufficientFundCheckRequest;
import jt.skunkworks.dataflow.message.SufficientFundCheckRequestBuilder;
import jt.skunkworks.fx.Order;
import lombok.SneakyThrows;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Profile("localTest")
@Configuration
public class MockConfig {

    @Bean
    public ClientAndServer startMockServer() {
        return startClientAndServer(8091);
    }

    @Bean
    public CommandLineRunner setupMockRequest(ClientAndServer clientAndServer, ObjectMapper objectMapper) {
        return (args) -> {
            mock(clientAndServer, objectMapper, "00000000-0000-0000-0000-000000000000", "APPROVED");
            mock(clientAndServer, objectMapper, "00000000-0000-0000-0000-000000000001", "INSUFFICIENT");
        };
    }

    @SneakyThrows
    private void mock(ClientAndServer clientAndServer, ObjectMapper objectMapper, String accountId, String result) {
        MockMessageBuilder builder = new MockMessageBuilder();
        Order order = builder.accountId(accountId).buildOrder();
        SufficientFundCheckRequest request = new SufficientFundCheckRequestBuilder(order).build();
        String json = objectMapper.writeValueAsString(request);

        clientAndServer.when(request()
                .withMethod("POST")
                .withPath("/fund/" + accountId + "/fundSufficientCheck")
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(json))
                .respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody("{\"result\": \""+result+"\"}")
                );

    }
}
