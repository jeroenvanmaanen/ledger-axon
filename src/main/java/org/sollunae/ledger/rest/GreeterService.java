package org.sollunae.ledger.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

@Component
public class GreeterService implements GreeterApiDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @PostConstruct
    public void postConstruct() {
        LOGGER.info("Post construct: {}", getClass());
    }

    @Override
    public ResponseEntity<String> prod(String name) {
        return ResponseEntity.of(Optional.of("Hello, " + name + "!"));
    }
}
