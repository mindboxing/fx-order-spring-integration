package jt.skunkworks.dataflow.config.gateway.bean;

import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig(classes = BeanGatewayTest.Config.class)
public class BeanGatewayTest {

    @Autowired
    OrderService orderService;
    @Autowired
    ApplicationContext context;

    @Test
    void test() {
        AccountValidationRequest input = new AccountValidationRequest();
        input.setAccountId("TEST-ID-0001");
        input.setAmount((double)100000);
        AccountValidationResponse response = orderService.doSomething(input);
        assertNotNull(response);
        System.out.println("!!! "+response);
        Stream.of(context.getBeanDefinitionNames()).forEach(System.out::println);
    }

    @Configuration
    @IntegrationComponentScan
    @EnableIntegration
    static class Config {
        @Bean
        SimpleOrderService simpleOrderService() {
            return new SimpleOrderService();
        }

        @Bean
        IntegrationFlow simpleFlow(SimpleOrderService service) {
            return f -> f.handle(service);
        }
    }
}

class SimpleOrderService {
    public AccountValidationResponse doSomething(AccountValidationRequest input) {
        AccountValidationResponse response = new AccountValidationResponse();
        response.setAccountId(input.getAccountId());
        response.setValid(true);
        return response;
    }
}

@MessagingGateway
interface OrderService {
    @Gateway(requestChannel = "simpleFlow.input")
    AccountValidationResponse doSomething(AccountValidationRequest input);
}

@Data
class AccountValidationRequest {
    String accountId;
    Double amount;
}

@Data
class AccountValidationResponse {
    String accountId;
    Boolean valid;
}