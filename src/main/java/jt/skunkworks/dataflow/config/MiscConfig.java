package jt.skunkworks.dataflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.integration.xml.transformer.UnmarshallingTransformer;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MiscConfig {
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory();
        httpRequestFactory.setReadTimeout(2000);
        return new RestTemplate(httpRequestFactory);
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerSpec poller() {
        return Pollers.fixedRate(500)
                .errorChannel("errorChannel");
    }

    @Bean
    public UnmarshallingTransformer unmarshal() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setContextPath("jt.skunkworks.fx");
        return new UnmarshallingTransformer(jaxb2Marshaller);
    }

    @Bean
    public RequestHandlerRetryAdvice retryAdvice() {
        Map<Class<? extends Throwable>, Boolean> exceptions = new HashMap<>();
        exceptions.put(SocketTimeoutException.class, true);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3,
                exceptions,
                true
        );

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();
        advice.setRetryTemplate(retryTemplate);
        return advice;
    }

    @Bean
    public ExpressionEvaluatingHeaderValueMessageProcessor orderId() {
        return new ExpressionEvaluatingHeaderValueMessageProcessor<String>("headers['id']", String.class);
    }

}
