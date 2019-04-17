package org.sollunae.ledger.axon.account.process;

import org.axonframework.queryhandling.QueryHandler;
import org.sollunae.ledger.axon.account.persistence.AccountDocument;
import org.sollunae.ledger.axon.account.persistence.LedgerAccountRepository;
import org.sollunae.ledger.axon.account.query.AccountAllQuery;
import org.sollunae.ledger.axon.account.query.AccountByIdQuery;
import org.sollunae.ledger.model.ArrayOfAccountData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccountQueryHandler {

    private final LedgerAccountRepository ledgerAccountRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public AccountQueryHandler(LedgerAccountRepository ledgerAccountRepository, MongoTemplate mongoTemplate) {
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @QueryHandler
    public ArrayOfAccountData query(AccountAllQuery query) {
        List<AccountDocument> accounts = ledgerAccountRepository.findAll();
        ArrayOfAccountData accountsArray = new ArrayOfAccountData();
        accountsArray.addAll(accounts.stream().map(AccountDocument::getData).collect(Collectors.toList()));
        return accountsArray;
    }

    @QueryHandler
    public AccountDocument query(AccountByIdQuery query) {
        String accountId = query.getId();
        Query dbQuery = Query.query(Criteria.where("id").is(accountId));
        return mongoTemplate.findOne(dbQuery, AccountDocument.class);
    }
}
