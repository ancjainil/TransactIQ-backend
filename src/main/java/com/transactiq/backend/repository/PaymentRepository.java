package com.transactiq.backend.repository;

import com.transactiq.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByFromAccountId(Long fromAccountId);
    List<Payment> findByToAccountId(Long toAccountId);
    List<Payment> findByFromAccountIdOrToAccountId(Long fromAccountId, Long toAccountId);
}

