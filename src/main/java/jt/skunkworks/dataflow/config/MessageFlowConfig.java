package jt.skunkworks.dataflow.config;

import com.mongodb.MongoClientSettings;
import jt.skunkworks.dataflow.message.FxOrder;
import jt.skunkworks.dataflow.message.event.Event;
import jt.skunkworks.dataflow.service.FxOrderTransformer;
import lombok.extern.slf4j.Slf4j;
import org.bson.UuidRepresentation;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.dsl.*;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.mongodb.store.MongoDbChannelMessageStore;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.integration.selector.PayloadTypeSelector;
import org.springframework.integration.xml.transformer.UnmarshallingTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import static com.mongodb.MongoCredential.createCredential;
import static com.mongodb.client.MongoClients.create;


@Configuration
@Slf4j
public class MessageFlowConfig {

    // rabbitmq  ------------------------
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        return new CachingConnectionFactory("localhost");
    }

    @Bean
    public Queue fxPosition() {
        return new Queue("fx.order");
    }

    @Bean
    public Queue fxEvent() {
        return new Queue("fx.event");
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("skunkworks");
    }

    @Bean
    public Binding fxPositionBinding(Queue fxPosition, TopicExchange exchange) {
        return BindingBuilder
                .bind(fxPosition)
                .to(exchange)
                .with( "skunkworks:fx.order");
    }

    @Bean
    public Binding fxEventBinding(Queue fxEvent, TopicExchange exchange) {
        return BindingBuilder
                .bind(fxEvent)
                .to(exchange)
                .with( "skunkworks:fx.event");
    }

    // mongo ------------------------
    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory() {
        MongoClientSettings settings = MongoClientSettings
                .builder()
                .credential(createCredential("fxdbuser", "fxdb", "fxdbuser".toCharArray()))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build();
        return new SimpleMongoClientDatabaseFactory(create(settings), "fxdb");
    }

    @Bean
    public MongoDbChannelMessageStore mongoDbChannelMessageStore(MongoDatabaseFactory databaseFactory) {
        return new MongoDbChannelMessageStore(databaseFactory);
    }


    @Bean
    public MessageChannel persistChannel(MongoDbChannelMessageStore store) {
        return MessageChannels
                .queue(store, "fx-order-group")
                .get();
    }

    // misc -----------------------------------
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
    public void idempotentReceiver() {
        // TODO
//        MetadataStoreSelector selector = new MetadataStoreSelector();
//
//        IdempotentReceiverInterceptor interceptor = new IdempotentReceiverInterceptor(selector);
//        interceptor.setDiscardChannelName("duplicate.order");

    }

    // integration flow ------------------------
    @Bean
    public IntegrationFlow mainFlow(
            ConnectionFactory connectionFactory,
            Queue fxPosition,
            AmqpTemplate amqpTemplate
    ) {
        return IntegrationFlows
                .from(Amqp.inboundAdapter(connectionFactory, fxPosition))
                // persist message to prevent message lost
                .channel("persistChannel")
                // XML to Object
                .transform(unmarshal())
                // Object to Normalized entity object
                .transform(new FxOrderTransformer())
                // Call REST with retry
                .handle("validateService", "validate", config -> config.advice(retryAdvice()))
                // Generate Event Message
                .<FxOrder>handle((payload, headers) -> new Event(
                        payload.getOrderId().toString(),
                        payload.isFunded() ? Event.EventType.ORDER_ACCEPTED : Event.EventType.ORDER_REJECTED,
                        payload
                ))
                // Transform object to Json
                .transform(new ObjectToJsonTransformer())
                // Send to fx.event topic
                .handle(Amqp.outboundAdapter(amqpTemplate).routingKey("fx.event"))
                .get();
    }

    @Bean
    public IntegrationFlow error() {
        return IntegrationFlows
                .from("errorChannel")
                .handle(s -> {
                    ErrorMessage errorMessage = (ErrorMessage) s;
                    Message<?> originalMessage = errorMessage.getOriginalMessage();
                    if (originalMessage != null) {
                        byte[] payload = (byte[])originalMessage.getPayload();
                        log.error("!!! MESSAGE \n{}", new String(payload));
                        MessageHeaders headers = originalMessage.getHeaders();
                        log.error("  > Header order-id: {}", headers.get("order-id"));
                    }
                    Throwable throwable = errorMessage.getPayload();
                    // save payload
                    log.error("!!! ERROR {}", throwable.getMessage());
                })
                .get();
    }


}
