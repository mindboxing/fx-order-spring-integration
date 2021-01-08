package jt.skunkworks.dataflow.config;

import com.mongodb.MongoClientSettings;
import org.bson.UuidRepresentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.mongodb.metadata.MongoDbMetadataStore;
import org.springframework.integration.mongodb.store.MongoDbChannelMessageStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.integration.transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.messaging.MessageChannel;

import static com.mongodb.MongoCredential.createCredential;
import static com.mongodb.client.MongoClients.create;

@Configuration
public class MongoConfig {
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

    @Bean
    public IdempotentReceiverInterceptor idempotentReceiver(
            MongoDatabaseFactory dataStore,
            ExpressionEvaluatingHeaderValueMessageProcessor messageProcessor
    ) {
        MetadataStoreSelector selector = new MetadataStoreSelector(
                messageProcessor,
                new MongoDbMetadataStore(dataStore)
        );
        IdempotentReceiverInterceptor interceptor = new IdempotentReceiverInterceptor(selector);
        return interceptor;
    }

}
