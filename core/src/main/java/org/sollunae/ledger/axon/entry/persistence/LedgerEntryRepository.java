package org.sollunae.ledger.axon.entry.persistence;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.util.List;

public interface LedgerEntryRepository extends PagingAndSortingRepository<EntryDocument,String> {

    List<EntryDocument> findByDataKey(String key);

    @Query(value = "{'$and':[{'data.date':{'$gte':?0}},{'data.date':{'$lt':?1}}]}", sort = "{'data.key':1}")
    Iterable<EntryDocument> findByDateBetween(LocalDate from, LocalDate to);
}
