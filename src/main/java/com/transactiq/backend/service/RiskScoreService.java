package com.transactiq.backend.service;

import com.transactiq.backend.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Service for calculating risk scores for payments
 * Risk score ranges from 0-100 (higher = more risk)
 */
@Service
@RequiredArgsConstructor
public class RiskScoreService {
    
    // Risk thresholds for auto-approval
    private static final BigDecimal AUTO_APPROVE_MAX_RISK = BigDecimal.valueOf(30); // Auto-approve if risk <= 30
    private static final BigDecimal AUTO_APPROVE_MAX_AMOUNT = BigDecimal.valueOf(1000); // Auto-approve if amount <= $1000
    
    /**
     * Calculate risk score for a payment (0-100)
     * Higher score = higher risk
     */
    public BigDecimal calculateRiskScore(Payment payment) {
        BigDecimal riskScore = BigDecimal.ZERO;
        
        // 1. Amount Risk (0-30 points)
        BigDecimal amountRisk = calculateAmountRisk(payment.getAmount());
        riskScore = riskScore.add(amountRisk);
        
        // 2. Currency Risk (0-20 points)
        BigDecimal currencyRisk = calculateCurrencyRisk(payment);
        riskScore = riskScore.add(currencyRisk);
        
        // 3. Transfer Type Risk (0-15 points)
        BigDecimal transferTypeRisk = calculateTransferTypeRisk(payment);
        riskScore = riskScore.add(transferTypeRisk);
        
        // 4. Time Risk (0-10 points)
        BigDecimal timeRisk = calculateTimeRisk(payment.getCreatedAt());
        riskScore = riskScore.add(timeRisk);
        
        // 5. Account Balance Risk (0-15 points)
        BigDecimal balanceRisk = calculateBalanceRisk(payment);
        riskScore = riskScore.add(balanceRisk);
        
        // 6. User History Risk (0-10 points)
        BigDecimal historyRisk = calculateHistoryRisk(payment);
        riskScore = riskScore.add(historyRisk);
        
        // Cap at 100
        if (riskScore.compareTo(BigDecimal.valueOf(100)) > 0) {
            riskScore = BigDecimal.valueOf(100);
        }
        
        return riskScore.setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateAmountRisk(BigDecimal amount) {
        // Amount thresholds
        if (amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            return BigDecimal.ZERO; // < $1,000 = 0 points
        } else if (amount.compareTo(BigDecimal.valueOf(10000)) < 0) {
            return BigDecimal.valueOf(5); // $1K-$10K = 5 points
        } else if (amount.compareTo(BigDecimal.valueOf(50000)) < 0) {
            return BigDecimal.valueOf(15); // $10K-$50K = 15 points
        } else if (amount.compareTo(BigDecimal.valueOf(100000)) < 0) {
            return BigDecimal.valueOf(25); // $50K-$100K = 25 points
        } else {
            return BigDecimal.valueOf(30); // > $100K = 30 points
        }
    }
    
    private BigDecimal calculateCurrencyRisk(Payment payment) {
        // Different currency = higher risk
        String fromCurrency = payment.getFromAccount().getCurrency();
        String toCurrency = payment.getToAccount().getCurrency();
        
        if (!fromCurrency.equalsIgnoreCase(toCurrency)) {
            return BigDecimal.valueOf(20); // International transfer
        }
        return BigDecimal.ZERO; // Same currency
    }
    
    private BigDecimal calculateTransferTypeRisk(Payment payment) {
        // External transfers = higher risk
        if (payment.getTransferType() == Payment.TransferType.EXTERNAL) {
            return BigDecimal.valueOf(15);
        }
        return BigDecimal.ZERO; // Internal transfer
    }
    
    private BigDecimal calculateTimeRisk(LocalDateTime createdAt) {
        // Unusual hours = higher risk (2 AM - 6 AM)
        // If createdAt is null (payment not yet saved), use current time
        LocalDateTime timeToCheck = createdAt != null ? createdAt : LocalDateTime.now();
        LocalTime time = timeToCheck.toLocalTime();
        int hour = time.getHour();
        if (hour >= 2 && hour < 6) {
            return BigDecimal.valueOf(10);
        }
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateBalanceRisk(Payment payment) {
        // Low balance after payment = higher risk
        BigDecimal currentBalance = payment.getFromAccount().getBalance();
        BigDecimal paymentAmount = payment.getAmount();
        BigDecimal remainingBalance = currentBalance.subtract(paymentAmount);
        
        // If remaining balance < 10% of payment amount
        BigDecimal tenPercent = paymentAmount.multiply(BigDecimal.valueOf(0.1));
        if (remainingBalance.compareTo(tenPercent) < 0) {
            return BigDecimal.valueOf(15);
        }
        
        // If remaining balance < 50% of payment amount
        BigDecimal fiftyPercent = paymentAmount.multiply(BigDecimal.valueOf(0.5));
        if (remainingBalance.compareTo(fiftyPercent) < 0) {
            return BigDecimal.valueOf(8);
        }
        
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateHistoryRisk(Payment payment) {
        // First payment to this recipient = higher risk
        // This would require checking payment history
        // For now, return 0 (can be enhanced later)
        return BigDecimal.ZERO;
    }
    
    /**
     * Get risk level from risk score
     */
    public Payment.RiskLevel getRiskLevel(BigDecimal riskScore) {
        if (riskScore.compareTo(BigDecimal.valueOf(30)) <= 0) {
            return Payment.RiskLevel.LOW;
        } else if (riskScore.compareTo(BigDecimal.valueOf(60)) <= 0) {
            return Payment.RiskLevel.MEDIUM;
        } else if (riskScore.compareTo(BigDecimal.valueOf(80)) <= 0) {
            return Payment.RiskLevel.HIGH;
        } else {
            return Payment.RiskLevel.VERY_HIGH;
        }
    }
    
    /**
     * Check if payment should be auto-approved based on risk score and amount
     * Auto-approve if:
     * - Risk score <= 30 AND amount <= $1000
     * - OR risk score <= 20 (very low risk, regardless of amount up to $10K)
     */
    public boolean shouldAutoApprove(Payment payment, BigDecimal riskScore) {
        // Very low risk (<= 20) - auto-approve up to $10K
        if (riskScore.compareTo(BigDecimal.valueOf(20)) <= 0) {
            return payment.getAmount().compareTo(BigDecimal.valueOf(10000)) <= 0;
        }
        
        // Low risk (21-30) AND small amount (<= $1000) - auto-approve
        if (riskScore.compareTo(AUTO_APPROVE_MAX_RISK) <= 0) {
            return payment.getAmount().compareTo(AUTO_APPROVE_MAX_AMOUNT) <= 0;
        }
        
        // Medium or higher risk - requires manual approval
        return false;
    }
}

