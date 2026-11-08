package com.ledgerline.service;

import com.ledgerline.model.IdempotencyRecord;
import com.ledgerline.repository.IdempotencyRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Checks if the key was already processed or currently processing.
     * Uses a separate transaction propagation to ensure in-flight locks are saved even if outer transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyRecord> checkAndLockKey(String key, String requestPayloadHash) {
        if (key == null || key.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return existing;
        }

        try {
            IdempotencyRecord newRecord = new IdempotencyRecord(key, requestPayloadHash, "PROCESSING");
            repository.saveAndFlush(newRecord);
            return Optional.empty(); // Key locked successfully, caller should proceed
        } catch (DataIntegrityViolationException ex) {
            // Concurrent request created key between find and save
            return repository.findByIdempotencyKey(key);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String key, int httpStatus, String responseBody) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        repository.findByIdempotencyKey(key).ifPresent(record -> {
            record.setStatus("COMPLETED");
            record.setHttpStatus(httpStatus);
            record.setResponseBody(responseBody);
            repository.save(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseLockOnFailure(String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        repository.findByIdempotencyKey(key).ifPresent(record -> {
            if ("PROCESSING".equals(record.getStatus())) {
                repository.delete(record);
            }
        });
    }
}

