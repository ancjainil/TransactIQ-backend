package com.transactiq.backend.service;

import com.transactiq.backend.entity.Account;
import com.transactiq.backend.entity.Payment;
import com.transactiq.backend.repository.AccountRepository;
import com.transactiq.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    
    public Payment createPayment(Payment payment) {
        // Generate transaction ID if not provided
        if (payment.getTransactionId() == null || payment.getTransactionId().isEmpty()) {
            payment.setTransactionId(generateTransactionId());
        }
        
        // Check if transaction ID already exists
        if (paymentRepository.findByTransactionId(payment.getTransactionId()).isPresent()) {
            throw new RuntimeException("Transaction ID already exists: " + payment.getTransactionId());
        }
        
        // Validate accounts exist
        Account fromAccount = accountRepository.findById(payment.getFromAccount().getId())
                .orElseThrow(() -> new RuntimeException("From account not found"));
        
        Account toAccount = accountRepository.findById(payment.getToAccount().getId())
                .orElseThrow(() -> new RuntimeException("To account not found"));
        
        // Validate accounts are active
        if (!fromAccount.getIsActive()) {
            throw new RuntimeException("From account is not active");
        }
        if (!toAccount.getIsActive()) {
            throw new RuntimeException("To account is not active");
        }
        
        // Validate sufficient balance
        if (fromAccount.getBalance().compareTo(payment.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance in from account");
        }
        
        payment.setFromAccount(fromAccount);
        payment.setToAccount(toAccount);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        
        return paymentRepository.save(payment);
    }
    
    public Payment processPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));
        
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new RuntimeException("Payment is not in PENDING status");
        }
        
        try {
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            paymentRepository.save(payment);
            
            // Deduct from sender account
            Account fromAccount = payment.getFromAccount();
            BigDecimal newFromBalance = fromAccount.getBalance().subtract(payment.getAmount());
            fromAccount.setBalance(newFromBalance);
            accountRepository.save(fromAccount);
            
            // Add to receiver account
            Account toAccount = payment.getToAccount();
            BigDecimal newToBalance = toAccount.getBalance().add(payment.getAmount());
            toAccount.setBalance(newToBalance);
            accountRepository.save(toAccount);
            
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setProcessedAt(LocalDateTime.now());
            
            return paymentRepository.save(payment);
            
        } catch (Exception e) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }
    
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
    
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }
    
    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }
    
    public List<Payment> getPaymentsByAccountId(Long accountId) {
        return paymentRepository.findByFromAccountIdOrToAccountId(accountId, accountId);
    }
    
    public List<Payment> getOutgoingPaymentsByAccountId(Long accountId) {
        return paymentRepository.findByFromAccountId(accountId);
    }
    
    public List<Payment> getIncomingPaymentsByAccountId(Long accountId) {
        return paymentRepository.findByToAccountId(accountId);
    }
    
    public Payment cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));
        
        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed payment");
        }
        
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        return paymentRepository.save(payment);
    }
    
    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

