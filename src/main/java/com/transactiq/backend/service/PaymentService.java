package com.transactiq.backend.service;

import com.transactiq.backend.entity.Account;
import com.transactiq.backend.entity.Payment;
import com.transactiq.backend.entity.User;
import com.transactiq.backend.repository.AccountRepository;
import com.transactiq.backend.repository.PaymentRepository;
import com.transactiq.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final UserRepository userRepository;
    private final ExchangeRateService exchangeRateService;
    
    public Payment createPayment(Payment payment, Long currentUserId) {
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
        
        // Validate fromAccount belongs to current user
        if (!fromAccount.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("From account does not belong to current user");
        }
        
        // Validate accounts are active
        if (!fromAccount.getIsActive()) {
            throw new RuntimeException("From account is not active");
        }
        if (!toAccount.getIsActive()) {
            throw new RuntimeException("To account is not active");
        }
        
        // Detect or validate transfer type
        boolean isInternal = fromAccount.getUser().getId().equals(toAccount.getUser().getId());
        Payment.TransferType detectedType = isInternal ? Payment.TransferType.INTERNAL : Payment.TransferType.EXTERNAL;
        
        // If transferType is provided, validate it matches actual account ownership
        if (payment.getTransferType() != null) {
            if (payment.getTransferType() == Payment.TransferType.INTERNAL && !isInternal) {
                throw new RuntimeException("Cannot mark as internal transfer. To account belongs to another user.");
            }
            if (payment.getTransferType() == Payment.TransferType.EXTERNAL && isInternal) {
                throw new RuntimeException("Cannot mark as external transfer. To account belongs to current user. Use internal transfer instead.");
            }
        }
        
        // Set transfer type (use provided or detected)
        payment.setTransferType(payment.getTransferType() != null ? payment.getTransferType() : detectedType);
        
        // Prevent self-transfer (same account)
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new RuntimeException("Cannot transfer to the same account");
        }
        
        // Note: We don't check balance here because payment is PENDING
        // Balance will be checked when payment is approved
        
        // Set currency from fromAccount
        payment.setCurrency(fromAccount.getCurrency());
        
        // Handle currency conversion if accounts have different currencies
        String fromCurrency = fromAccount.getCurrency();
        String toCurrency = toAccount.getCurrency();
        
        if (!fromCurrency.equalsIgnoreCase(toCurrency)) {
            try {
                // Get exchange rate and convert amount
                BigDecimal rate = exchangeRateService.getExchangeRate(fromCurrency, toCurrency);
                BigDecimal convertedAmount = exchangeRateService.convertAmount(
                    payment.getAmount(), 
                    fromCurrency, 
                    toCurrency
                );
                
                payment.setExchangeRate(rate);
                payment.setConvertedAmount(convertedAmount);
                payment.setConvertedCurrency(toCurrency);
            } catch (Exception e) {
                throw new RuntimeException(
                    String.format("Failed to convert currency from %s to %s: %s", 
                        fromCurrency, toCurrency, e.getMessage())
                );
            }
        } else {
            // Same currency, no conversion needed
            payment.setExchangeRate(BigDecimal.ONE);
            payment.setConvertedAmount(payment.getAmount());
            payment.setConvertedCurrency(toCurrency);
        }
        
        payment.setFromAccount(fromAccount);
        payment.setToAccount(toAccount);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        
        return paymentRepository.save(payment);
    }
    
    public Payment processPayment(Long paymentId) {
        // This method is kept for backward compatibility
        // Use approvePayment instead
        return approvePayment(paymentId, null);
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
        
        payment.setStatus(Payment.PaymentStatus.REJECTED);
        return paymentRepository.save(payment);
    }
    
    public Payment approvePayment(Long paymentId, Long approverUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));
        
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new RuntimeException("Payment is not in PENDING status");
        }
        
        // Validate sufficient balance
        Account fromAccount = payment.getFromAccount();
        String fromCurrency = fromAccount.getCurrency();
        BigDecimal amountToDeduct = payment.getAmount(); // Amount in fromAccount currency
        
        if (fromAccount.getBalance().compareTo(amountToDeduct) < 0) {
            throw new RuntimeException("Insufficient balance in from account");
        }
        
        // Transfer funds with currency conversion if needed
        Account toAccount = payment.getToAccount();
        String toCurrency = toAccount.getCurrency();
        
        // Deduct from sender account (in sender's currency)
        BigDecimal newFromBalance = fromAccount.getBalance().subtract(amountToDeduct);
        fromAccount.setBalance(newFromBalance);
        accountRepository.save(fromAccount);
        
        // Add to receiver account (in receiver's currency)
        BigDecimal amountToAdd;
        if (!fromCurrency.equalsIgnoreCase(toCurrency)) {
            // Use converted amount if currencies differ
            if (payment.getConvertedAmount() != null) {
                amountToAdd = payment.getConvertedAmount();
            } else {
                // Recalculate if not stored (shouldn't happen, but safety check)
                amountToAdd = exchangeRateService.convertAmount(
                    payment.getAmount(), 
                    fromCurrency, 
                    toCurrency
                );
                payment.setConvertedAmount(amountToAdd);
            }
        } else {
            // Same currency
            amountToAdd = payment.getAmount();
        }
        
        BigDecimal newToBalance = toAccount.getBalance().add(amountToAdd);
        toAccount.setBalance(newToBalance);
        accountRepository.save(toAccount);
        
        // Update payment status
        payment.setStatus(Payment.PaymentStatus.APPROVED);
        payment.setApprovedAt(LocalDateTime.now());
        
        // Set approver if provided
        if (approverUserId != null) {
            User approver = userRepository.findById(approverUserId)
                    .orElse(null);
            payment.setApprovedBy(approver);
        }
        
        return paymentRepository.save(payment);
    }
    
    public Payment rejectPayment(Long paymentId, Long rejectorUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));
        
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new RuntimeException("Payment is not in PENDING status");
        }
        
        // Update payment status (do NOT transfer funds)
        payment.setStatus(Payment.PaymentStatus.REJECTED);
        
        return paymentRepository.save(payment);
    }
    
    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

