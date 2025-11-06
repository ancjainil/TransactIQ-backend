package com.transactiq.backend.controller;

import com.transactiq.backend.entity.Payment;
import com.transactiq.backend.entity.User;
import com.transactiq.backend.repository.UserRepository;
import com.transactiq.backend.service.PaymentService;
import com.transactiq.backend.util.RoleUtil;
import com.transactiq.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for checker dashboard
 * Provides endpoints for checkers to view approval queue, risk statistics, etc.
 */
@RestController
@RequestMapping("/api/checker")
@RequiredArgsConstructor
public class CheckerDashboardController {
    
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    
    /**
     * Get approval queue for checkers
     * Shows pending payments sorted by risk score (highest first)
     */
    @GetMapping("/approval-queue")
    public ResponseEntity<?> getApprovalQueue() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Only checkers and admins can access
            if (!RoleUtil.canApprovePayments(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Only checkers and admins can access approval queue"));
            }
            
            // Get all pending payments
            List<Payment> allPayments = paymentService.getAllPayments();
            List<Payment> pendingPayments = allPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                    .sorted((p1, p2) -> {
                        // Sort by risk score (highest first), then by creation date (oldest first)
                        java.math.BigDecimal risk1 = p1.getRiskScore() != null ? p1.getRiskScore() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal risk2 = p2.getRiskScore() != null ? p2.getRiskScore() : java.math.BigDecimal.ZERO;
                        int riskCompare = risk2.compareTo(risk1);
                        if (riskCompare != 0) {
                            return riskCompare;
                        }
                        return p1.getCreatedAt().compareTo(p2.getCreatedAt());
                    })
                    .collect(Collectors.toList());
            
            // Format response with risk information
            List<Map<String, Object>> formattedPayments = pendingPayments.stream()
                    .map(payment -> {
                        Map<String, Object> payMap = new HashMap<>();
                        payMap.put("id", payment.getId());
                        payMap.put("transactionId", payment.getTransactionId());
                        payMap.put("amount", payment.getAmount());
                        payMap.put("currency", payment.getCurrency());
                        payMap.put("description", payment.getDescription());
                        payMap.put("riskScore", payment.getRiskScore() != null ? payment.getRiskScore() : 0);
                        payMap.put("riskLevel", payment.getRiskLevel() != null ? payment.getRiskLevel().name() : "LOW");
                        payMap.put("transferType", payment.getTransferType() != null ? payment.getTransferType().name() : "INTERNAL");
                        payMap.put("createdAt", payment.getCreatedAt());
                        
                        // From account info
                        Map<String, Object> fromAccount = new HashMap<>();
                        fromAccount.put("id", payment.getFromAccount().getId());
                        fromAccount.put("accountNumber", maskAccountNumber(payment.getFromAccount().getAccountNumber()));
                        fromAccount.put("currency", payment.getFromAccount().getCurrency());
                        fromAccount.put("balance", payment.getFromAccount().getBalance());
                        fromAccount.put("userName", getUserName(payment.getFromAccount().getUser()));
                        fromAccount.put("userEmail", payment.getFromAccount().getUser().getEmail());
                        payMap.put("fromAccount", fromAccount);
                        
                        // To account info
                        Map<String, Object> toAccount = new HashMap<>();
                        toAccount.put("id", payment.getToAccount().getId());
                        toAccount.put("accountNumber", maskAccountNumber(payment.getToAccount().getAccountNumber()));
                        toAccount.put("currency", payment.getToAccount().getCurrency());
                        toAccount.put("userName", getUserName(payment.getToAccount().getUser()));
                        toAccount.put("userEmail", payment.getToAccount().getUser().getEmail());
                        payMap.put("toAccount", toAccount);
                        
                        // Conversion info if applicable
                        if (payment.getExchangeRate() != null && payment.getExchangeRate().compareTo(java.math.BigDecimal.ONE) != 0) {
                            Map<String, Object> conversion = new HashMap<>();
                            conversion.put("exchangeRate", payment.getExchangeRate());
                            conversion.put("originalAmount", payment.getAmount());
                            conversion.put("originalCurrency", payment.getCurrency());
                            conversion.put("convertedAmount", payment.getConvertedAmount());
                            conversion.put("convertedCurrency", payment.getConvertedCurrency());
                            payMap.put("conversion", conversion);
                        }
                        
                        return payMap;
                    })
                    .collect(Collectors.toList());
            
            // Build dashboard response
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("pendingPayments", formattedPayments);
            dashboard.put("totalPending", pendingPayments.size());
            dashboard.put("highRiskCount", pendingPayments.stream()
                    .filter(p -> p.getRiskLevel() != null && 
                            (p.getRiskLevel() == Payment.RiskLevel.HIGH || 
                             p.getRiskLevel() == Payment.RiskLevel.VERY_HIGH))
                    .count());
            dashboard.put("mediumRiskCount", pendingPayments.stream()
                    .filter(p -> p.getRiskLevel() != null && p.getRiskLevel() == Payment.RiskLevel.MEDIUM)
                    .count());
            dashboard.put("lowRiskCount", pendingPayments.stream()
                    .filter(p -> p.getRiskLevel() == null || p.getRiskLevel() == Payment.RiskLevel.LOW)
                    .count());
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch approval queue: " + e.getMessage()));
        }
    }
    
    /**
     * Get risk statistics for checker dashboard
     */
    @GetMapping("/risk-statistics")
    public ResponseEntity<?> getRiskStatistics() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!RoleUtil.canApprovePayments(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Only checkers and admins can access risk statistics"));
            }
            
            List<Payment> allPayments = paymentService.getAllPayments();
            List<Payment> pendingPayments = allPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                    .collect(Collectors.toList());
            
            // Calculate statistics
            Map<String, Object> stats = new HashMap<>();
            
            // Risk level distribution
            long lowRisk = pendingPayments.stream()
                    .filter(p -> p.getRiskLevel() == null || p.getRiskLevel() == Payment.RiskLevel.LOW)
                    .count();
            long mediumRisk = pendingPayments.stream()
                    .filter(p -> p.getRiskLevel() == Payment.RiskLevel.MEDIUM)
                    .count();
            long highRisk = pendingPayments.stream()
                    .filter(p -> p.getRiskLevel() == Payment.RiskLevel.HIGH)
                    .count();
            long veryHighRisk = pendingPayments.stream()
                    .filter(p -> p.getRiskLevel() == Payment.RiskLevel.VERY_HIGH)
                    .count();
            
            stats.put("lowRisk", lowRisk);
            stats.put("mediumRisk", mediumRisk);
            stats.put("highRisk", highRisk);
            stats.put("veryHighRisk", veryHighRisk);
            
            // Average risk score
            double avgRiskScore = pendingPayments.stream()
                    .filter(p -> p.getRiskScore() != null)
                    .mapToDouble(p -> p.getRiskScore().doubleValue())
                    .average()
                    .orElse(0.0);
            stats.put("averageRiskScore", Math.round(avgRiskScore * 100.0) / 100.0);
            
            // Total pending amount
            double totalPendingAmount = pendingPayments.stream()
                    .mapToDouble(p -> p.getAmount().doubleValue())
                    .sum();
            stats.put("totalPendingAmount", totalPendingAmount);
            
            // Auto-approved count (for reference)
            long autoApprovedCount = allPayments.stream()
                    .filter(p -> p.getAutoApproved() != null && p.getAutoApproved() && 
                            p.getStatus() == Payment.PaymentStatus.APPROVED)
                    .count();
            stats.put("autoApprovedCount", autoApprovedCount);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch risk statistics: " + e.getMessage()));
        }
    }
    
    /**
     * Get checker dashboard summary
     * Shows overview for checkers when they log in
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getCheckerDashboard() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!RoleUtil.canApprovePayments(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Only checkers and admins can access checker dashboard"));
            }
            
            // Get approval queue data
            ResponseEntity<?> queueResponse = getApprovalQueue();
            if (queueResponse.getStatusCode() != HttpStatus.OK) {
                return queueResponse;
            }
            
            // Get risk statistics
            ResponseEntity<?> statsResponse = getRiskStatistics();
            if (statsResponse.getStatusCode() != HttpStatus.OK) {
                return statsResponse;
            }
            
            // Combine into dashboard
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("approvalQueue", queueResponse.getBody());
            dashboard.put("riskStatistics", statsResponse.getBody());
            dashboard.put("userRole", RoleUtil.getRoleLowercase(user));
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch checker dashboard: " + e.getMessage()));
        }
    }
    
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****" + accountNumber;
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
    
    private String getUserName(User user) {
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + 
                         " " + (user.getLastName() != null ? user.getLastName() : "");
        fullName = fullName.trim();
        return fullName.isEmpty() ? user.getUsername() : fullName;
    }
}

