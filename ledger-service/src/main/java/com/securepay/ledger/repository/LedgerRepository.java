package com.securepay.ledger.repository;

import com.securepay.ledger.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByUserId(Long userId);
    Optional<LedgerEntry> findByTransactionIdAndEntryTypeAndUserId(Long transactionId, String entryType, Long userId);
}
