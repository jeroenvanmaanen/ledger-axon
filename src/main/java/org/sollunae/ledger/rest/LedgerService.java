package org.sollunae.ledger.rest;

import io.swagger.model.Jar;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class LedgerService implements LedgerApiDelegate {

    @Override
    public ResponseEntity<String> assignEntryToJar(String id, Jar jar) {
        return null;
    }

    @Override
    public ResponseEntity<String> createEntry() {
        return null;
    }
}
