package org.sollunae.ledger.axon.unique.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.LedgerCommandGateway;
import org.sollunae.ledger.axon.unique.command.AddUniqueKeyCommand;
import org.sollunae.ledger.axon.unique.command.CreateUniqueBucketCommand;
import org.sollunae.ledger.axon.unique.command.UniqueBucketLogStatisticsCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class UniqueKeyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String UNIQUE_ROOT_ID = "unique-root";
    private final LedgerCommandGateway commandGateway;
    private final HashingMethod hashingMethod;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    @Autowired
    public UniqueKeyService(LedgerCommandGateway commandGateway, HashingMethod hashingMethod) {
        this.commandGateway = commandGateway;
        this.hashingMethod = hashingMethod;
    }

    public void assertUnique(Object domain, String key) {
        if (!isInitialized.getAndSet(true)) {
            initialize();
        }
        String hash = hashingMethod.createHash(domain, key);
        LOGGER.trace("Assert unique: {}: {}: {}", domain, key, hash);
        String result = commandGateway.sendAndWait(AddUniqueKeyCommand.builder()
            .id(UNIQUE_ROOT_ID)
            .domain(domain)
            .key(key)
            .hash(hash)
            .build());
        if (StringUtils.isEmpty(result)) {
            LOGGER.debug("Key already exists: {}: {}", domain, key);
            throw new IllegalStateException("Key already exists: " + domain + ": " + key);
        }
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
