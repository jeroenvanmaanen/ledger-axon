package org.sollunae.ledger.axon.account.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface LedgerAccountRepository extends MongoRepository<AccountDocument,String> {
}
