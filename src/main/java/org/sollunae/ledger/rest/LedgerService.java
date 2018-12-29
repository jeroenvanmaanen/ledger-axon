package org.sollunae.ledger.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sollunae.ledger.axon.account.command.CreateAccountCommand;
import org.sollunae.ledger.axon.compound.command.CreateCompoundCommand;
import org.sollunae.ledger.axon.entry.command.CreateEntryCommand;
import org.sollunae.ledger.axon.entry.command.EntryAddToCompoundCommand;
import org.sollunae.ledger.axon.entry.command.EntryRemoveFromCompoundCommand;
import org.sollunae.ledger.model.AccountData;
import org.sollunae.ledger.model.EntryData;
import org.sollunae.ledger.model.JarData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.sollunae.ledger.util.StreamUtil.asStream;

@Component
public class LedgerService implements LedgerApiDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;
    private final Map<String,CSVFormat> csvFormatMap = new HashMap<>();
    private final Map<String, BiConsumer<EntryData,String>> stringSetterMap = new HashMap<>();
    private final Map<String, BiConsumer<EntryData,Integer>> integerSetterMap = new HashMap<>();
    private final Map<String, BiConsumer<EntryData,LocalDate>> dateSetterMap = new HashMap<>();

    public LedgerService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
        for (CSVFormat.Predefined csvFormat : CSVFormat.Predefined.values()) {
            csvFormatMap.put(csvFormat.name(), csvFormat.getFormat());
        }
        //Datum|Naam / Omschrijving|Rekening|Tegenrekening|Code|Af Bij|Bedrag (EUR)|MutatieSoort|Mededelingen
        dateSetterMap.put("Datum", EntryData::setDate);
        stringSetterMap.put("Naam / Omschrijving", EntryData::setDescription);
        stringSetterMap.put("Rekening", EntryData::setAccount);
        stringSetterMap.put("Tegenrekening", EntryData::setContraAccount);
        stringSetterMap.put("Code", EntryData::setCode);
        stringSetterMap.put("Af Bij", EntryData::setDebetCredit);
        integerSetterMap.put("Bedrag (EUR)", EntryData::setAmountCents);
        stringSetterMap.put("MutatieSoort", EntryData::setKind);
        stringSetterMap.put("Mededelingen", EntryData::setRemarks);
    }

    @Override
    public ResponseEntity<Void> assignEntryToJar(String id, JarData jar) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
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
    public ResponseEntity<String> createCompound() {
        String id = UUID.randomUUID().toString();
        Object createCommand = CreateCompoundCommand.builder()
            .id(id)
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
                        LOGGER.warn("Exception while importing entry: {}", exception);
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
        for (CSVRecord row : rows) {
            try {
                LOGGER.info("Row: {}", asStream(row).collect(Collectors.joining("|")));
                EntryData entryData = mapRow(row);
                String id = UUID.randomUUID().toString();
                commandGateway.sendAndWait(CreateEntryCommand.builder().id(id).entry(entryData).build());
                imported++;
            } catch (RuntimeException exception) {
                LOGGER.warn("Exception while importing entry: {}", exception);
                failed++;
            }
        }
        LOGGER.info("Entries imported: {}: failed: {}", imported, failed);
    }

    private EntryData mapRow(CSVRecord row) {
        EntryData entryData = new EntryData();
        for (Map.Entry<String,String> entry : row.toMap().entrySet()) {
            if (mapTo(entryData, stringSetterMap, Function.identity(), entry) ||
                mapTo(entryData, integerSetterMap, this::toInteger, entry) ||
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

    private Integer toInteger(String value) {
        return Integer.parseInt(value.replaceAll("[^0-9]", "").replaceAll("^$", "0"));
    }

    private LocalDate toLocalDate(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeException exception) {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
    }
}
