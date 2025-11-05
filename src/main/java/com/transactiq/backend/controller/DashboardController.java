package com.transactiq.backend.controller;

import com.transactiq.backend.entity.Account;
import com.transactiq.backend.entity.Payment;
import com.transactiq.backend.service.AccountService;
import com.transactiq.backend.service.PaymentService;
import com.transactiq.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final AccountService accountService;
    private final PaymentService paymentService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Unauthorized"));
            }
            
            // Get user's accounts
            List<Account> accounts = accountService.getActiveAccountsByUserId(userId);
            
            // Calculate total balance
            BigDecimal totalBalance = accounts.stream()
                    .map(Account::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Get all payments for user's accounts
            List<Payment> allPayments = accounts.stream()
                    .flatMap(account -> paymentService.getPaymentsByAccountId(account.getId()).stream())
                    .toList();
            
            // Get recent payments count
            long recentTransactionsCount = allPayments.size();
            
            // Count pending and approved payments
            long pendingPayments = allPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                    .count();
            
            long approvedPayments = allPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.APPROVED || 
                                p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .count();
            
            // Calculate currency balances
            Map<String, BigDecimal> currencyBalances = new HashMap<>();
            for (Account account : accounts) {
                String currency = account.getCurrency() != null ? account.getCurrency() : "USD";
                currencyBalances.merge(currency, account.getBalance(), BigDecimal::add);
            }
            
            // Convert to list with percentages
            List<Map<String, Object>> currencyBalancesList = currencyBalances.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> currencyMap = new HashMap<>();
                        currencyMap.put("currency", entry.getKey());
                        currencyMap.put("balance", entry.getValue());
                        // Calculate percentage of total balance
                        double percentage = totalBalance.compareTo(BigDecimal.ZERO) > 0
                                ? entry.getValue().divide(totalBalance, 4, java.math.RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                                : 0.0;
                        currencyMap.put("percentage", Math.round(percentage * 100.0) / 100.0);
                        return currencyMap;
                    })
                    .sorted((a, b) -> ((BigDecimal) b.get("balance")).compareTo((BigDecimal) a.get("balance")))
                    .toList();
            
            // Build response matching API spec
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalBalance", totalBalance);
            dashboard.put("recentTransactions", (int) recentTransactionsCount);
            dashboard.put("activeAccounts", accounts.size());
            dashboard.put("pendingPayments", (int) pendingPayments);
            dashboard.put("approvedPayments", (int) approvedPayments);
            dashboard.put("currencyBalances", currencyBalancesList);
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch dashboard data: " + e.getMessage()));
        }
    }
    
    // Keep old endpoint for backward compatibility
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getDashboardDataLegacy(@PathVariable Long userId) {
        try {
            List<Account> accounts = accountService.getActiveAccountsByUserId(userId);
            
            BigDecimal totalBalance = accounts.stream()
                    .map(Account::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long recentTransactionsCount = accounts.stream()
                    .flatMap(account -> paymentService.getPaymentsByAccountId(account.getId()).stream())
                    .count();
            
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalBalance", totalBalance);
            dashboard.put("recentTransactions", (int) recentTransactionsCount);
            dashboard.put("activeAccounts", accounts.size());
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch dashboard data: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("message", message);
        return error;
    }
}

