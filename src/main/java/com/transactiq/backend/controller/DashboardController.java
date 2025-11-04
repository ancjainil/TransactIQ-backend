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
            
            // Get recent payments count for all user accounts
            long recentTransactionsCount = accounts.stream()
                    .flatMap(account -> paymentService.getPaymentsByAccountId(account.getId()).stream())
                    .count();
            
            // Build response matching API spec
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalBalance", totalBalance);
            dashboard.put("recentTransactions", (int) recentTransactionsCount);
            dashboard.put("activeAccounts", accounts.size());
            
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

