package org.sollunae.ledger.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.account.command.CreateAccountCommand;
import org.sollunae.ledger.axon.account.query.AccountAllQuery;
import org.sollunae.ledger.axon.compound.command.CompoundUpdateIntendedJarCommand;
import org.sollunae.ledger.axon.compound.command.CreateCompoundCommand;
import org.sollunae.ledger.axon.compound.persistence.CompoundDocument;
import org.sollunae.ledger.axon.compound.persistence.LedgerCompoundRepository;
import org.sollunae.ledger.axon.compound.query.CompoundByIdQuery;
import org.sollunae.ledger.axon.entry.query.EntriesWithDatePrefixQuery;
import org.sollunae.ledger.axon.entry.command.CreateEntryCommand;
import org.sollunae.ledger.axon.entry.command.EntryAddToCompoundCommand;
import org.sollunae.ledger.axon.entry.command.EntryRemoveFromCompoundCommand;
import org.sollunae.ledger.axon.entry.persistence.EntryDocument;
import org.sollunae.ledger.axon.entry.persistence.LedgerEntryRepository;
import org.sollunae.ledger.axon.entry.query.EntryByIdQuery;
import org.sollunae.ledger.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.sollunae.ledger.util.StreamUtil.asStream;
import static org.sollunae.ledger.util.StringUtil.isNotEmpty;

@Component
public class LedgerService implements LedgerApiDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private final LedgerEntryRepository entryRepository;
    private final LedgerCompoundRepository compoundRepository;
    private final Map<String,CSVFormat> csvFormatMap = new HashMap<>();
    private final Map<String, BiConsumer<EntryData,String>> stringSetterMap = new HashMap<>();
    private final Map<String, BiConsumer<EntryData,LocalDate>> dateSetterMap = new HashMap<>();

    public LedgerService(CommandGateway commandGateway, QueryGateway queryGateway, LedgerEntryRepository entryRepository, LedgerCompoundRepository compoundRepository) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
        this.entryRepository = entryRepository;
        this.compoundRepository = compoundRepository;
        for (CSVFormat.Predefined csvFormat : CSVFormat.Predefined.values()) {
            csvFormatMap.put(csvFormat.name(), csvFormat.getFormat());
        }
        dateSetterMap.put("Datum", EntryData::setDate);
        stringSetterMap.put("Naam / Omschrijving", EntryData::setDescription);
        stringSetterMap.put("Rekening", EntryData::setAccount);
        stringSetterMap.put("Tegenrekening", EntryData::setContraAccount);
        stringSetterMap.put("Code", EntryData::setCode);
        stringSetterMap.put("Af Bij", EntryData::setDebetCredit);
        stringSetterMap.put("Bedrag (EUR)", EntryData::setAmount);
        stringSetterMap.put("MutatieSoort", EntryData::setKind);
        stringSetterMap.put("Mededelingen", EntryData::setRemarks);
    }

    @Override
    public ResponseEntity<Void> assignEntryToJar(String id, JarData jar) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }

    @Override
    public ResponseEntity<ArrayOfAccountData> getAllAccounts() {
        try {
            ArrayOfAccountData accounts = queryGateway.query(new AccountAllQuery(), ArrayOfAccountData.class).get();
            return ResponseEntity.ok(accounts);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error getting Accounts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Override
    public ResponseEntity<ArrayOfEntryData> getEntriesWithPrefix(String datePrefix) {
        try {
            EntriesWithDatePrefixQuery query = EntriesWithDatePrefixQuery.builder()
                .datePrefix(datePrefix)
                .build();
            ArrayOfEntryData accounts = queryGateway.query(query, ArrayOfEntryData.class).get();
            return ResponseEntity.ok(accounts);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error getting entries with date prefix: {}", datePrefix, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Override
    public ResponseEntity<String> createAccount(String id, AccountData data) {
        if (!Objects.equals(id, data.getAccount())) {
            throw new HttpServerErrorException(HttpStatus.BAD_REQUEST);
        }
        Object createCommand = CreateAccountCommand.builder()
            .id(id)
            .data(data)
            .build();
        try {
            commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(createCommand));
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        } catch (RuntimeException exception) {
            LOGGER.error("Exception during command execution: {}", exception.getCause(), exception);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    @Override
    public ResponseEntity<String> createEntry(EntryData entry) {
        String id = UUID.randomUUID().toString();
        Object createCommand = CreateEntryCommand.builder()
            .id(id)
            .entry(entry)
            .build();
        try {
            commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(createCommand));
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        } catch (RuntimeException exception) {
            LOGGER.error("Exception during command execution: {}", exception.getCause(), exception);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    @Override
    public ResponseEntity<EntryData> getEntry(String id) {
        try {
            EntryData result = queryGateway.query(EntryByIdQuery.builder().entryId(id).build(), EntryData.class).get();
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            } else {
                return ResponseEntity.ok(result);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error getting Entry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Override
    public ResponseEntity<String> createCompound() {
        try {
            String id = createCompoundTransaction();
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        } catch (RuntimeException exception) {
            LOGGER.error("Exception during command execution: {}", exception.getCause(), exception);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    private String createCompoundTransaction() {
        String id = UUID.randomUUID().toString();
        Object createCommand = CreateCompoundCommand.builder()
            .id(id)
            .build();
        commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(createCommand));
        return id;
    }

    @Override
    public ResponseEntity<CompoundData> getCompound(String  compoundId) {
        try {
            CompoundData result = queryGateway.query(CompoundByIdQuery.builder().compoundId(compoundId).build(), CompoundData.class).get();
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            } else {
                return ResponseEntity.ok(result);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error getting Compound", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Override
    public ResponseEntity<Void> addEntryToCompound(String id, String compoundId) {
        EntryAddToCompoundCommand entryAddToCompoundCommand = EntryAddToCompoundCommand.builder()
            .id(id)
            .compoundId(compoundId)
            .build();
        commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(entryAddToCompoundCommand));
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<Void> removeEntryFromCompound(String id) {
        EntryRemoveFromCompoundCommand entryRemoveFromCompoundCommand = EntryRemoveFromCompoundCommand.builder()
            .id(id)
            .build();
        commandGateway.sendAndWait(GenericCommandMessage.asCommandMessage(entryRemoveFromCompoundCommand));
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<Void> addMemberToCompound(String compoundId, CompoundMemberData member) {
        EntryDocument entry = null;
        if (isNotEmpty(member.getId())) {
            entry = entryRepository.findById(member.getId()).orElse(null);
        } else if (isNotEmpty(member.getKey())) {
            entry = Optional.ofNullable(entryRepository.findByDataKey(member.getKey()))
                .map(Collection::stream)
                .flatMap(Stream::findFirst)
                .orElse(null);
        }
        if (entry == null) {
            LOGGER.warn("Not found: {}: {}", member.getId(), member.getKey());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } else {
            if (member.getAmountCents() != null && !Objects.equals(member.getAmountCents(), entry.getData().getAmountCents())) {
                LOGGER.warn("Amount mismatch: {}: {}: {}: {}", member.getId(), member.getKey(), member.getAmountCents(), entry.getData().getAmountCents());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            } else if (isNotEmpty(member.getJar()) && !Objects.equals(member.getJar(), entry.getData().getJar())) {
                LOGGER.warn("JAR mismatch: {}: {}: {}: {}", member.getId(), member.getKey(), member.getJar(), entry.getData().getJar());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            } else if (isNotEmpty(member.getContraJar()) && !Objects.equals(member.getContraJar(), entry.getData().getContraJar())) {
                LOGGER.warn("Contra-JAR mismatch: {}: {}: {}: {}", member.getId(), member.getKey(), member.getContraJar(), entry.getData().getContraJar());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            addEntryToCompound(entry.getId(), compoundId);
        }
        return ResponseEntity.ok(null);
    }

    @Override
    public ResponseEntity<Void> setIntendedJar(String id, JarData intendedJar) {
        commandGateway.sendAndWait(CompoundUpdateIntendedJarCommand.builder()
            .id(id)
            .intendedJar(intendedJar.getCode())
            .build()
        );
        return null;
    }

    @Override
    public ResponseEntity<Void> uploadCompoundTransactions(MultipartFile data) {
        LOGGER.info("Upload accounts YAML");
        try {
            uploadCompoundTransactions(data.getInputStream());
            return ResponseEntity.ok(null);
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Exception while uploading accounts YAML", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private void uploadCompoundTransactions(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            String line;
            StringBuilder builder = new StringBuilder();
            int imported = 0;
            int failed = 0;
            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    try {
                        CompoundData compound = mapper.readValue(builder.toString(), CompoundData.class);
                        builder = new StringBuilder();
                        String compoundId = null;
                        if (isNotEmpty(compound.getId())) {
                            LOGGER.info("Import compound with Id: {}", compound.getId());
                            compoundId = compound.getId();
                        } else if (isNotEmpty(compound.getKey())) {
                            LOGGER.info("Import compound with key: {}", compound.getId());
                            compoundId = getCompoundIdByKey(compound.getKey());
                        }
                        if (StringUtils.isEmpty(compoundId)) {
                            compoundId = createCompoundTransaction();
                            LOGGER.info("Created compound with Id: {}", compoundId);
                        } else {
                            LOGGER.info("Update existing compound with Id: {}", compoundId);
                        }
                        compound.setId(compoundId);
                        LOGGER.info("Import compound: {}: {}", compound.getId(), compound.getKey());
                        String intendedJar = compound.getIntendedJar();
                        if (intendedJar != null) {
                            LOGGER.debug("Set intended jar: {}: {}", compoundId, intendedJar);
                            CompoundUpdateIntendedJarCommand command = CompoundUpdateIntendedJarCommand.builder()
                                .id(compoundId)
                                .intendedJar(intendedJar)
                                .build();
                            commandGateway.sendAndWait(command);
                        }
                        int failedMembers = 0;
                        for (CompoundMemberData member : compound.getMembers().values()) {
                            if (addMemberToCompound(compoundId, member).getStatusCodeValue() > 299) {
                                failedMembers++;
                            }
                        }
                        if (failedMembers > 0) {
                            throw new IllegalStateException("Failed to add members: " + failedMembers);
                        }
                        imported++;
                    } catch (RuntimeException exception) {
                        LOGGER.warn("Exception while importing compound: {}", exception.toString());
                        failed++;
                    }
                } else {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(line);
                }
            }
            LOGGER.info("Compound transactions imported: {}: failed: {}", imported, failed);
        }
    }

    private String getCompoundIdByKey(String key) {
        return Optional.ofNullable(compoundRepository.findByKey(key))
            .map(Collection::stream)
            .flatMap(Stream::findFirst)
            .map(CompoundDocument::getId)
            .orElse(null);
    }

    @Override
    public ResponseEntity<Void> uploadAccounts(MultipartFile data) {
        LOGGER.info("Upload accounts YAML");
        try {
            uploadAccounts(data.getInputStream());
            return ResponseEntity.ok(null);
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Exception while uploading accounts YAML", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private void uploadAccounts(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            String line;
            StringBuilder builder = new StringBuilder();
            int imported = 0;
            int failed = 0;
            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    try {
                        AccountData account = mapper.readValue(builder.toString(), AccountData.class);
                        builder = new StringBuilder();
                        LOGGER.info("Import account: {}: {}", account.getAccount(), account.getKey());
                        Object createAccountCommand = CreateAccountCommand.builder()
                            .id(account.getAccount())
                            .data(account)
                            .build();
                        commandGateway.sendAndWait(createAccountCommand);
                        imported++;
                    } catch (RuntimeException exception) {
                        LOGGER.warn("Exception while importing account: {}", exception.toString());
                        failed++;
                    }
                } else {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(line);
                }
            }
            LOGGER.info("Accounts imported: {}: failed: {}", imported, failed);
        }
    }

    @Override
    public ResponseEntity<Void> uploadEntries(String format, MultipartFile data) {
        LOGGER.info("Upload entries CSV");
        try {
            uploadEntries(format, data.getInputStream());
            return ResponseEntity.ok(null);
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Exception while uploading entries CSV", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private void uploadEntries(String format, InputStream stream) throws IOException {
        Reader reader = new InputStreamReader(stream);
        CSVFormat csvFormat = csvFormatMap.get(format);
        if (csvFormat == null) {
            throw new RuntimeException("Unknown CSV format: " + format);
        }
        csvFormat = csvFormat.withFirstRecordAsHeader();
        Iterable<CSVRecord> rows = csvFormat.parse(reader);
        int imported = 0;
        int failed = 0;
        LocalDate lastDate = LocalDate.MIN;
        int sequence = 0;
        for (CSVRecord row : rows) {
            try {
                LOGGER.info("Row: {}", asStream(row).collect(Collectors.joining("|")));
                EntryData entryData = mapRow(row);
                String id = UUID.randomUUID().toString();
                if (Objects.equals(entryData.getDate(), lastDate)) {
                    sequence++;
                } else {
                    lastDate = entryData.getDate();
                    sequence = 1;
                }
                String key = DateTimeFormatter.ISO_LOCAL_DATE.format(lastDate) + "_" + sequence;
                entryData.setKey(key);
                commandGateway.sendAndWait(CreateEntryCommand.builder().id(id).entry(entryData).build());
                imported++;
            } catch (RuntimeException exception) {
                LOGGER.warn("Exception while importing entry: {}", exception.toString());
                failed++;
            }
        }
        LOGGER.info("Entries imported: {}: failed: {}", imported, failed);
    }

    private EntryData mapRow(CSVRecord row) {
        EntryData entryData = new EntryData();
        for (Map.Entry<String,String> entry : row.toMap().entrySet()) {
            if (mapTo(entryData, stringSetterMap, Function.identity(), entry) ||
                mapTo(entryData, dateSetterMap, this::toLocalDate, entry)) {
                LOGGER.trace("Mapped: {}: {}", entry.getKey(), entry.getValue());
            } else {
                LOGGER.warn("Unable to map: {}, {}", entry.getKey(), entry.getValue());
            }
        }
        return entryData;
    }

    private <T> boolean mapTo(EntryData entry, Map<String,BiConsumer<EntryData,T>> setterMap, Function<String,T> converter, Map.Entry<String,String> pair) {
        return Optional.ofNullable(setterMap.get(pair.getKey()))
            .map(setter -> {
                T value = converter.apply(pair.getValue());
                setter.accept(entry, value);
                return true;
            })
            .orElse(false);
    }

    private LocalDate toLocalDate(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeException exception) {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
    }
}
