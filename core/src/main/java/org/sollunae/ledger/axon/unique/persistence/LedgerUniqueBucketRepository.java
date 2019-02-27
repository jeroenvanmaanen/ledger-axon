package org.sollunae.ledger.axon.unique.persistence;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface LedgerUniqueBucketRepository extends PagingAndSortingRepository<UniqueBucketDocument,String> {

    @Query("{" +
        "    $expr: { $and: [" +
        "        { $lte: ['$fullPrefix', ?0] }," +
        "        { $gt:  [{$concat: ['$fullPrefix', '~']}, ?0] }" +
        "    ] }" +
        "}")
    List<UniqueBucketDocument> findBucketsForHash(String hash);
}
