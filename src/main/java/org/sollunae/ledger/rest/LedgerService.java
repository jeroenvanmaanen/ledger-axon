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
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.sollunae.ledger.util.StreamUtil.asStream;

@Component
public class LedgerService implements LedgerApiDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;

    public LedgerService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
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
            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    AccountData account = mapper.readValue(builder.toString(), AccountData.class);
                    builder = new StringBuilder();
                    LOGGER.info("Import account: {}: {}", account.getAccount(), account.getKey());
                    Object createAccountCommand = CreateAccountCommand.builder()
                        .id(account.getAccount())
                        .data(account)
                        .build();
                    commandGateway.sendAndWait(createAccountCommand);
                } else {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(line);
                }
            }
        }
    }

    @Override
    public ResponseEntity<Void> uploadEntries(MultipartFile data) {
        LOGGER.info("Upload entries CSV");
        try {
            uploadEntries(data.getInputStream());
            return ResponseEntity.ok(null);
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Exception while uploading entries CSV", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private void uploadEntries(InputStream stream) throws IOException {
        Reader reader = new InputStreamReader(stream);
        Iterable<CSVRecord> rows = CSVFormat.EXCEL.parse(reader);
        for (CSVRecord row : rows) {
            LOGGER.info("Row: {}", asStream(row).collect(Collectors.joining("|")));
        }
    }

    private void unwrapIOException(RuntimeException exception) throws IOException {
        Throwable t = exception;
        while (t != null) {
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            t = t.getCause();
        }
        throw exception;
    }
}
