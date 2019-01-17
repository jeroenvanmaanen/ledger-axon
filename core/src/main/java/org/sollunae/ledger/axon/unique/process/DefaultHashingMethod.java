package org.sollunae.ledger.axon.unique.process;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class DefaultHashingMethod implements HashingMethod {

    @Override
    public String createHash(Object domain, String key) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
        hash = Base64.getEncoder().encode(hash);
        return new String(hash, StandardCharsets.UTF_8);
    }
}
