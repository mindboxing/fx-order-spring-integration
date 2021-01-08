package jt.skunkworks.dataflow.config;

import jt.skunkworks.dataflow.message.FundCheckResult;
import jt.skunkworks.dataflow.message.SufficientFundCheckRequest;
import jt.skunkworks.dataflow.message.SufficientFundCheckRequestBuilder;
import jt.skunkworks.dataflow.message.SufficientFundCheckResponse;
import jt.skunkworks.dataflow.message.event.Event;
import jt.skunkworks.fx.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.http.dsl.Http;
import org.springframework.integration.xml.transformer.UnmarshallingTransformer;
import org.springframework.web.client.RestTemplate;

import static jt.skunkworks.dataflow.message.event.Event.EventType.ORDER_ACCEPTED;
import static jt.skunkworks.dataflow.message.event.Event.EventType.ORDER_REJECTED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
@Slf4j
public class MessageFlowConfig {

    @Bean
    public IntegrationFlow processOrderFlow(UnmarshallingTransformer unmarshal) {
        return IntegrationFlows.from("process.input")
                .transform(unmarshal)
                .enrichHeaders(c -> c
                        .headerExpression("accountId", "payload.accountId")
                        .headerExpression("orderId", "payload.orderId")
                )
                .gateway("fundSufficientCheck.input", c -> c.errorChannel("errorChannel"))
                .<SufficientFundCheckResponse>handle((resp, header) -> {
                    return new Event(header.get("orderId").toString(),
                            resp.getResult() == FundCheckResult.APPROVED
                                    ? ORDER_ACCEPTED : ORDER_REJECTED
                    );
                })
                .transform(Transformers.toJson())
                .channel("process.output")
                .get();
    }

    @Bean
    public IntegrationFlow fundSufficientCheck(
            RestTemplate restTemplate,
            RequestHandlerRetryAdvice retryAdvice
    ) {
        return flow -> flow
                .<Order, SufficientFundCheckRequest>transform(payload -> new SufficientFundCheckRequestBuilder(payload).build())
                .transform(Transformers.toJson())
                .enrichHeaders(c -> c.header("contentType", APPLICATION_JSON_VALUE, true))
                .handle(Http
                                .outboundGateway("http://localhost:8091/fund/{accountId}/fundSufficientCheck", restTemplate)
                                .expectedResponseType(SufficientFundCheckResponse.class)
                                .httpMethod(HttpMethod.POST)
                                .extractPayload(true)
                                .uriVariable("accountId", "headers['accountId']"),
                        c -> c.advice(retryAdvice)
                )
                ;
    }


    @Bean
    public IntegrationFlow error() {
        return IntegrationFlows
                .from("errorChannel")
                .log(LoggingHandler.Level.ERROR, "APP-ERROR")
                .channel("deadLetterQueue")
                .get();
    }
}
