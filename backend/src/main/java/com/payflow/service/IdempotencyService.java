package com.payflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.entity.IdempotencyKey;
import com.payflow.entity.IdempotencyStatus;
import com.payflow.exception.DuplicateRequestException;
import com.payflow.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Attempts to acquire an idempotency lock for the given key and payload.
     * Runs in a separate transaction (REQUIRES_NEW) to commit the PROCESSING lock immediately.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyKey> checkAndAcquire(String key, String endpoint, Object payload) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        String payloadHash = calculateHash(payload);

        Optional<IdempotencyKey> existingOpt = idempotencyRepository.findByIdempotencyKey(key);
        if (existingOpt.isPresent()) {
            IdempotencyKey existing = existingOpt.get();
            if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
                log.info("Idempotent hit: returning cached response for key: {}", key);
                return Optional.of(existing);
            }
            if (existing.getStatus() == IdempotencyStatus.PROCESSING) {
                log.warn("Concurrent duplicate request detected for key: {}", key);
                throw new DuplicateRequestException("Duplicate transaction in progress for idempotency key: " + key);
            }
        }

        try {
            IdempotencyKey newRecord = IdempotencyKey.builder()
                    .idempotencyKey(key)
                    .endpoint(endpoint)
                    .requestHash(payloadHash)
                    .status(IdempotencyStatus.PROCESSING)
                    .build();
            IdempotencyKey saved = idempotencyRepository.saveAndFlush(newRecord);
            return Optional.of(saved);
        } catch (DataIntegrityViolationException e) {
            // Unique constraint violation in case another thread inserted right between check and save
            throw new DuplicateRequestException("Duplicate request in progress for key: " + key);
        }
    }

    /**
     * Marks the idempotency key as COMPLETED and caches the serialized response payload.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String key, Object responseObject, int statusCode) {
        if (key == null || key.isBlank()) return;

        idempotencyRepository.findByIdempotencyKey(key).ifPresent(record -> {
            try {
                record.setResponsePayload(objectMapper.writeValueAsString(responseObject));
                record.setResponseStatusCode(statusCode);
                record.setStatus(IdempotencyStatus.COMPLETED);
                idempotencyRepository.save(record);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize cached response for idempotency key: {}", key, e);
            }
        });
    }

    /**
     * Marks the idempotency key as FAILED if the transaction fails due to business error.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String key) {
        if (key == null || key.isBlank()) return;
        idempotencyRepository.findByIdempotencyKey(key).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            idempotencyRepository.save(record);
        });
    }

    private String calculateHash(Object payload) {
        try {
            String json = payload != null ? objectMapper.writeValueAsString(payload) : "";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            return "UNHASHED_PAYLOAD";
        }
    }
}
