package org.sollunae.ledger.axon.account.process;

import org.axonframework.queryhandling.QueryHandler;
import org.sollunae.ledger.axon.account.persistence.AccountDocument;
import org.sollunae.ledger.axon.account.persistence.LedgerAccountRepository;
import org.sollunae.ledger.axon.account.query.AccountAllQuery;
import org.sollunae.ledger.model.ArrayOfAccountData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccountQueryHandler {

    private final LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    public AccountQueryHandler(LedgerAccountRepository ledgerAccountRepository) {
        this.ledgerAccountRepository = ledgerAccountRepository;
    }

    @QueryHandler
    public ArrayOfAccountData query(AccountAllQuery query, MongoTemplate mongoTemplate) {
        List<AccountDocument> accounts = ledgerAccountRepository.findAll();
        ArrayOfAccountData accountsArray = new ArrayOfAccountData();
        accountsArray.addAll(accounts.stream().map(AccountDocument::getData).collect(Collectors.toList()));
        return accountsArray;
    }
}
