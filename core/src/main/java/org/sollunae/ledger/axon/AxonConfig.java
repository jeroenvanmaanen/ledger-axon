package org.sollunae.ledger.axon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.mongodb.MongoClient;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.gateway.CommandGatewayFactory;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.extensions.mongo.DefaultMongoTemplate;
import org.axonframework.extensions.mongo.MongoTemplate;
import org.axonframework.extensions.mongo.eventsourcing.tokenstore.MongoTokenStore;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.spring.config.annotation.AnnotationCommandHandlerBeanPostProcessor;
import org.sollunae.ledger.axon.LedgerCommandGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AxonConfig {
    private static final String TRACKING_TOKENS_COLLECTION = "axon-tracking-tokens";
    private static final String SAGAS_COLLECTION = "axon-sagas";

    @Bean
    public Serializer eventSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass annotatedClass) {
                if (annotatedClass.hasAnnotation(JsonPOJOBuilder.class)) {
                    return super.findPOJOBuilderConfig(annotatedClass);
                }
                // If no annotation present use default as empty prefix
                return new JsonPOJOBuilder.Value("build", "");
            }
        });
        return JacksonSerializer.builder().objectMapper(objectMapper).build();
    }

    @Bean
    public MongoTemplate axonMongoTemplate(MongoClient mongoClient) {
        return DefaultMongoTemplate.builder()
            .mongoDatabase(mongoClient)
            .trackingTokensCollectionName(TRACKING_TOKENS_COLLECTION)
            .sagasCollectionName(SAGAS_COLLECTION)
            .build();
    }

    @Bean
    public TokenStore tokenStore(MongoTemplate mongoTemplate, Serializer serializer) {
        return MongoTokenStore.builder()
            .mongoTemplate(mongoTemplate)
            .serializer(serializer)
            .build();
    }

    @Bean
    public CommandGatewayFactory commandGatewayFactory(CommandBus commandBus) {
        return CommandGatewayFactory.builder()
            .commandBus(commandBus)
            .build();
    }

    @Bean
    public LedgerCommandGateway ledgerCommandGateway(CommandGatewayFactory commandGatewayFactory) {
        return commandGatewayFactory.createGateway(LedgerCommandGateway.class);
    }

    @Bean
    public AnnotationCommandHandlerBeanPostProcessor annotationCommandHandlerBeanPostProcessor() {
        return new AnnotationCommandHandlerBeanPostProcessor();
    }
}
