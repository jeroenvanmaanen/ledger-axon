package org.sollunae.ledger.axon.compound.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface LedgerCompoundRepository extends PagingAndSortingRepository<CompoundDocument,String> {
    List<CompoundDocument> findByKey(String key);
}
