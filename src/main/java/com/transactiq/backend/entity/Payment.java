package com.transactiq.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;
    
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @NotBlank
    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD"; // Currency of the payment amount (from account currency)
    
    @Column(name = "converted_amount", precision = 19, scale = 2)
    private BigDecimal convertedAmount; // Amount in to account currency (if different)
    
    @Column(name = "converted_currency", length = 3)
    private String convertedCurrency; // Currency of converted amount (to account currency)
    
    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate; // Exchange rate used for conversion
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "transfer_type", length = 20)
    @Enumerated(EnumType.STRING)
    private TransferType transferType = TransferType.INTERNAL; // Default to internal
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;
    
    public enum PaymentStatus {
        PENDING,
        APPROVED,
        REJECTED,
        COMPLETED
    }
    
    public enum TransferType {
        INTERNAL,
        EXTERNAL
    }
}

