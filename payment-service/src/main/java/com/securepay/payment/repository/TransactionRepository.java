package com.securepay.payment.repository;

import com.securepay.payment.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByCorrelationId(String correlationId);
    List<Transaction> findByFromUserIdOrToUserId(Long fromUserId, Long toUserId);
}
