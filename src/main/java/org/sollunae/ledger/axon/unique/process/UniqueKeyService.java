package org.sollunae.ledger.axon.unique.process;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.unique.command.AddUniqueKeyCommand;
import org.sollunae.ledger.axon.unique.command.CreateUniqueBucketCommand;
import org.sollunae.ledger.axon.unique.command.UniqueBucketLogStatisticsCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class UniqueKeyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String UNIQUE_ROOT_ID = "unique-root";
    private final CommandGateway commandGateway;
    private final HashingMethod hashingMethod;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    @Autowired
    public UniqueKeyService(CommandGateway commandGateway, HashingMethod hashingMethod) {
        this.commandGateway = commandGateway;
        this.hashingMethod = hashingMethod;
    }

    public void assertUnique(Object domain, String key) {
        if (!isInitialized.getAndSet(true)) {
            initialize();
        }
        String hash = hashingMethod.createHash(domain, key);
        commandGateway.sendAndWait(AddUniqueKeyCommand.builder()
            .id(UNIQUE_ROOT_ID)
            .domain(domain)
            .key(key)
            .hash(hash)
            .build());
    }

    private void initialize() {
        try {
            LOGGER.info("Initializing: {}", UNIQUE_ROOT_ID);
            commandGateway.sendAndWait(CreateUniqueBucketCommand.builder()
                .id(UNIQUE_ROOT_ID).maxKeys(100).childKeyPrefixLength(2).build());
        } catch (Exception ignore) {
            LOGGER.info("Unique bucket root already exists");
        }
        commandGateway.sendAndWait(UniqueBucketLogStatisticsCommand.builder().id(UNIQUE_ROOT_ID).build());
    }
}
