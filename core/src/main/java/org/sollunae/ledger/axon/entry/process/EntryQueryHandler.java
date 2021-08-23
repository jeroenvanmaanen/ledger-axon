package org.sollunae.ledger.axon.entry.process;

import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.entry.persistence.EntryDocument;
import org.sollunae.ledger.axon.entry.persistence.LedgerEntryRepository;
import org.sollunae.ledger.axon.entry.query.EntriesWithDatePrefixQuery;
import org.sollunae.ledger.axon.entry.query.EntryByIdQuery;
import org.sollunae.ledger.axon.entry.query.LastEntryQuery;
import org.sollunae.ledger.model.ArrayOfEntryData;
import org.sollunae.ledger.model.EntryData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class EntryQueryHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final LedgerEntryRepository ledgerEntryRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public EntryQueryHandler(LedgerEntryRepository ledgerEntryRepository, MongoTemplate mongoTemplate) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @QueryHandler
    public EntryData query(EntryByIdQuery query) {
        Query mongoQuery = Query.query(Criteria.where("_id").is(query.getEntryId()));
        return Optional.ofNullable(mongoTemplate.findOne(mongoQuery, EntryDocument.class))
            .map(EntryDocument::getData)
            .orElse(null);
    }

    @QueryHandler
    public ArrayOfEntryData query(EntriesWithDatePrefixQuery query) {
        try {
            LocalDate from = getFromDate(query.getDatePrefix());
            LocalDate to = getToDate(query.getDatePrefix());
            Iterable<EntryDocument> entries = ledgerEntryRepository.findByDateBetween(from, to);
            ArrayOfEntryData entriesArray = new ArrayOfEntryData();
            entriesArray.addAll(StreamSupport.stream(entries.spliterator(), false)
                .map(EntryDocument::getData).collect(Collectors.toList()));
            return entriesArray;
        } catch (RuntimeException exception) {
            LOGGER.warn("Exception in query", exception);
            throw exception;
        }
    }

    @QueryHandler
    public EntryData query(LastEntryQuery query) {
        try {
            Sort sort = Sort.by(Sort.Direction.DESC, "data.date");
            return StreamSupport.stream(ledgerEntryRepository.findAll(sort).spliterator(), false)
                .findFirst()
                .map(EntryDocument::getData)
                .orElse(null);
        } catch (RuntimeException exception) {
            LOGGER.warn("Exception in query", exception);
            throw exception;
        }
    }

    private LocalDate getFromDate(String datePrefix) {
        String year = datePrefix.replaceAll("[^0-9].*", "");
        String monthDay = datePrefix.replaceAll("^[0-9]*-?", "");
        LOGGER.trace("Month day: {}", monthDay);
        if (monthDay.length() < 5) {
            monthDay = monthDay + "00-00".substring(monthDay.length());
            monthDay = monthDay.replaceAll("^00-", "01-");
            monthDay = monthDay.replaceAll("-00$", "-01");
            LOGGER.trace("Month day: {}", monthDay);
        }
        String dateString = year + "-" + monthDay;
        LOGGER.trace("From date: Date prefix: {}: Date string: {}", datePrefix, dateString);
        return LocalDate.parse(dateString);
    }

    private LocalDate getToDate(String datePrefix) {
        LOGGER.trace("To date: date Prefix: {}", datePrefix);
        int factor = 1;
        if (datePrefix.matches("^.*-[0-9]$")) {
            factor = 10;
        }
        Collection<String> parts = Arrays.asList(datePrefix.split("-"));
        List<Integer> numbers = parts.stream().map(Integer::parseInt).collect(Collectors.toList());
        int index = numbers.size() - 1;
        while (numbers.size() < 3) {
            numbers.add(1);
        }
        while (numbers.size() > 3) {
            numbers.remove(3);
        }
        do {
            LOGGER.trace("Index: {}: Numbers[index]: {}: Factor: {}", index, numbers.get(index), factor);
            numbers.set(index, (numbers.get(index) + 1) * factor);
            try {
                List<String> newParts = numbers.stream().map(this::datePart).collect(Collectors.toList());
                String attempt = String.join("-", newParts);
                LOGGER.trace("To date attempt: {}", attempt);
                return LocalDate.parse(attempt);
            } catch (RuntimeException ignore) {
                // proceed with bigger unit of time
            }
            numbers.set(index, 1);
            factor = 1;
            index--;
        } while (index >= 0);
        throw new IllegalArgumentException("Could not compute end date for: " + datePrefix);
    }

    private String datePart(int number) {
        StringBuilder result = new StringBuilder();
        result.append(String.valueOf(number));
        while (result.length() < 2) {
            result.insert(0, "0");
        }
        return result.toString();
    }
}
