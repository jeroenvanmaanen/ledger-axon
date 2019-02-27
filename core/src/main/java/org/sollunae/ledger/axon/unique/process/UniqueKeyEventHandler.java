package org.sollunae.ledger.axon.unique.process;

import org.axonframework.eventhandling.EventHandler;
import org.sollunae.ledger.axon.unique.event.FullPrefixAddedEvent;
import org.sollunae.ledger.axon.unique.event.UniqueBucketChildAddedEvent;
import org.sollunae.ledger.axon.unique.persistence.LedgerUniqueBucketRepository;
import org.sollunae.ledger.axon.unique.persistence.UniqueBucketDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class UniqueKeyEventHandler {

    private final LedgerUniqueBucketRepository repository;
    private final MongoTemplate mongoTemplate;

    public UniqueKeyEventHandler(LedgerUniqueBucketRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @EventHandler
    public void on(UniqueBucketChildAddedEvent event) {
        String childId = event.getChildId();
        String parentId = event.getParentId();
        String keyPrefix = event.getKeyPrefix();
        String fullPrefix = event.getFullPrefix();

        Query query = Query.query(Criteria.where("id").is(childId));
        Update update = Update
            .update("id", childId)
            .set("parentId", parentId)
            .set("keyPrefix", keyPrefix)
            .set("fullPrefix", fullPrefix)
            .set("_class", UniqueBucketDocument.class.getCanonicalName());
        if (fullPrefix != null) {
            update.set("fullPrefix", fullPrefix);
        }
        mongoTemplate.upsert(query, update, UniqueBucketDocument.class);
    }

    @EventHandler
    public void on(FullPrefixAddedEvent event) {
        String childId = event.getId();
        String fullPrefix = event.getFullPrefix();

        Query query = Query.query(Criteria.where("id").is(childId));
        Update update = Update
            .update("id", childId)
            .set("fullPrefix", fullPrefix)
            .set("_class", UniqueBucketDocument.class.getCanonicalName());
        if (fullPrefix != null) {
            update.set("fullPrefix", fullPrefix);
        }
        mongoTemplate.upsert(query, update, UniqueBucketDocument.class);
    }
}
