package org.sollunae.ledger.axon.compound.process;

import org.axonframework.queryhandling.QueryHandler;
import org.sollunae.ledger.axon.compound.persistence.CompoundDocument;
import org.sollunae.ledger.axon.compound.query.CompoundByIdQuery;
import org.sollunae.ledger.model.CompoundData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CompoundQueryHandler {
    private final MongoTemplate mongoTemplate;

    @Autowired
    public CompoundQueryHandler(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @QueryHandler
    public CompoundData query(CompoundByIdQuery query) {
        Query mongoQuery = Query.query(Criteria.where("_id").is(query.getCompoundId()));
        return Optional.ofNullable(mongoTemplate.findOne(mongoQuery, CompoundDocument.class))
            .map(this::toCompoundData)
            .orElse(null);
    }

    private CompoundData toCompoundData(CompoundDocument document) {
        CompoundData result = new CompoundData();
        result.setId(document.getId());
        result.setKey(document.getKey());
        result.setIntendedJar(document.getIntendedJar());
        result.setMembers(document.getMemberMap());
        result.setBalance(document.getBalance());
        result.setBalanceMatchesIntention(document.getBalanceMatchesIntention());
        return result;
    }
}
