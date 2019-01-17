package org.sollunae.ledger.axon.entry.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface LedgerEntryRepository extends PagingAndSortingRepository<EntryDocument,String> {
    List<EntryDocument> findByDataKey(String key);
}
