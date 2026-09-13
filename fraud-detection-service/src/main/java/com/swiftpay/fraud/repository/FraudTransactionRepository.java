package com.swiftpay.fraud.repository;

import com.swiftpay.fraud.entity.FraudTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FraudTransactionRepository
        extends JpaRepository<FraudTransaction, Long> {

    Optional<FraudTransaction> findByTransactionId(
            String transactionId
    );

    boolean existsByTransactionId(
            String transactionId
    );
}