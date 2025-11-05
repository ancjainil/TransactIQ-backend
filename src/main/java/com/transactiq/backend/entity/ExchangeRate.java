package com.transactiq.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Exchange rate entity for currency conversion
 * Stores the exchange rate from one currency to another
 */
@Entity
@Table(name = "exchange_rates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"from_currency", "to_currency"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "from_currency", length = 3, nullable = false)
    private String fromCurrency; // e.g., "USD"
    
    @NotBlank
    @Column(name = "to_currency", length = 3, nullable = false)
    private String toCurrency; // e.g., "CAD"
    
    @NotNull
    @DecimalMin(value = "0.0001", message = "Exchange rate must be greater than 0")
    @Column(name = "rate", nullable = false, precision = 19, scale = 6)
    private BigDecimal rate; // 1 fromCurrency = rate toCurrency
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Helper method to get the reverse rate (toCurrency to fromCurrency)
     */
    public BigDecimal getReverseRate() {
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE.divide(rate, 6, java.math.RoundingMode.HALF_UP);
    }
}

