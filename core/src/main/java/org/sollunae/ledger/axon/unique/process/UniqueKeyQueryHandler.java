package org.sollunae.ledger.axon.unique.process;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.sollunae.ledger.axon.unique.persistence.LedgerUniqueBucketRepository;
import org.sollunae.ledger.axon.unique.persistence.UniqueBucketDocument;
import org.sollunae.ledger.axon.unique.query.UniqueByHashQuery;
import org.sollunae.ledger.axon.unique.query.UniqueRootExists;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class UniqueKeyQueryHandler {

    private final LedgerUniqueBucketRepository repository;

    public UniqueKeyQueryHandler(LedgerUniqueBucketRepository repository) {
        this.repository = repository;
    }

    @QueryHandler
    public Boolean query(UniqueRootExists query) {
        return repository.findById(UniqueKeyService.UNIQUE_ROOT_ID).isPresent();
    }

    @QueryHandler
    public String query(UniqueByHashQuery query) {
        String hash = query.getHashCode();
        List<UniqueBucketDocument> buckets = repository.findBucketsForHash(hash);
        log.debug("Number of buckets for hash: {}: {}", hash, buckets.size());

        String result = UniqueKeyService.UNIQUE_ROOT_ID;
        int resultPrefixLength = 0;
        for (UniqueBucketDocument bucket : buckets) {
            String bucketPrefix = bucket.getFullPrefix();
            int bucketPrefixLength = bucketPrefix.length();
            if (bucketPrefixLength > resultPrefixLength && hash.startsWith(bucketPrefix)) {
                result = bucket.getId();
                resultPrefixLength = bucketPrefixLength;
            }
        }
        log.debug("Bucket for hash: {}: {}", hash, result);
        return result;
    }
}
