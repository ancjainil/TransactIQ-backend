package com.transactiq.backend.controller;

import com.transactiq.backend.entity.Account;
import com.transactiq.backend.service.AccountService;
import com.transactiq.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    
    @GetMapping
    public ResponseEntity<?> getAccounts() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            List<Account> accounts = accountService.getActiveAccountsByUserId(userId);
            
            // Format response to match API spec
            List<Map<String, Object>> formattedAccounts = accounts.stream()
                    .map(acc -> {
                        Map<String, Object> accMap = new HashMap<>();
                        accMap.put("id", acc.getId());
                        accMap.put("name", acc.getAccountType() + " Account"); // Or use account name if available
                        accMap.put("type", acc.getAccountType().toLowerCase());
                        accMap.put("balance", acc.getBalance());
                        accMap.put("currency", acc.getCurrency() != null ? acc.getCurrency() : "USD");
                        accMap.put("createdAt", acc.getCreatedAt() != null ? acc.getCreatedAt() : java.time.LocalDateTime.now());
                        // Mask account number - show last 4 digits
                        String accountNumber = acc.getAccountNumber();
                        String maskedNumber = accountNumber.length() > 4 
                            ? "****" + accountNumber.substring(accountNumber.length() - 4)
                            : "****" + accountNumber;
                        accMap.put("accountNumber", maskedNumber);
                        return accMap;
                    })
                    .toList();
            
            return ResponseEntity.ok(formattedAccounts);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to fetch accounts: " + e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createAccount(@RequestBody Map<String, Object> request) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            // Validate required fields
            if (!request.containsKey("type") || request.get("type") == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Account type is required"));
            }
            
            if (!request.containsKey("currency") || request.get("currency") == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Currency is required"));
            }
            
            // Validate account type enum
            String accountType = request.get("type").toString();
            if (!accountType.equalsIgnoreCase("checking") && 
                !accountType.equalsIgnoreCase("savings") && 
                !accountType.equalsIgnoreCase("business")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid account type. Must be: checking, savings, or business"));
            }
            
            // Validate currency enum
            String currency = request.get("currency").toString().toUpperCase();
            if (!currency.equals("USD") && !currency.equals("CAD") && !currency.equals("EUR")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid currency. Must be: USD, CAD, or EUR"));
            }
            
            // Create account entity
            Account account = new Account();
            account.setAccountType(accountType.toUpperCase());
            account.setCurrency(currency);
            
            // Set balance if provided, otherwise default to 0
            if (request.containsKey("balance") && request.get("balance") != null) {
                try {
                    Object balanceObj = request.get("balance");
                    BigDecimal balance;
                    if (balanceObj instanceof Number) {
                        balance = BigDecimal.valueOf(((Number) balanceObj).doubleValue());
                    } else {
                        balance = new BigDecimal(balanceObj.toString());
                    }
                    if (balance.compareTo(BigDecimal.ZERO) < 0) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "Balance cannot be negative"));
                    }
                    account.setBalance(balance);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid balance format"));
                }
            } else {
                account.setBalance(BigDecimal.ZERO);
            }
            
            // Set name if provided
            if (request.containsKey("name") && request.get("name") != null) {
                // Store name in description or use a separate field if available
                // For now, we'll use account type
            }
            
            // Create account
            Account createdAccount = accountService.createAccount(account, userId);
            
            // Format response
            Map<String, Object> response = new HashMap<>();
            response.put("id", createdAccount.getId());
            response.put("name", createdAccount.getAccountType() + " Account");
            response.put("type", createdAccount.getAccountType().toLowerCase());
            response.put("balance", createdAccount.getBalance());
            response.put("currency", createdAccount.getCurrency());
            response.put("createdAt", createdAccount.getCreatedAt());
            
            // Mask account number
            String accountNumber = createdAccount.getAccountNumber();
            String maskedNumber = accountNumber.length() > 4 
                ? "****" + accountNumber.substring(accountNumber.length() - 4)
                : "****" + accountNumber;
            response.put("accountNumber", maskedNumber);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to create account: " + e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Account>> getAccountsByUserId(@PathVariable Long userId) {
        List<Account> accounts = accountService.getAllAccountsByUserId(userId);
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }
    
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Account>> getActiveAccountsByUserId(@PathVariable Long userId) {
        List<Account> accounts = accountService.getActiveAccountsByUserId(userId);
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        return accountService.getAccountById(id)
                .map(account -> new ResponseEntity<>(account, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<Account> getAccountByAccountNumber(@PathVariable String accountNumber) {
        return accountService.getAccountByAccountNumber(accountNumber)
                .map(account -> new ResponseEntity<>(account, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(
            @PathVariable Long id,
            @RequestBody Account accountDetails) {
        try {
            Account updatedAccount = accountService.updateAccount(id, accountDetails);
            return new ResponseEntity<>(updatedAccount, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    /**
     * Search for external accounts by account number, email, user name, or account type
     * Excludes current user's accounts
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchAccounts(@RequestParam String q) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            // Validate query length (minimum 3 characters)
            if (q == null || q.trim().length() < 3) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "message", "Search query must be at least 3 characters",
                            "code", "SEARCH_QUERY_TOO_SHORT"
                        ));
            }
            
            // Search accounts (excluding current user's accounts)
            List<Account> accounts = accountService.searchAccountsExcludingUser(q.trim(), userId);
            
            // Limit to 10 results
            List<Account> limitedAccounts = accounts.stream()
                    .limit(10)
                    .toList();
            
            // Format response to match frontend requirements
            List<Map<String, Object>> formattedAccounts = limitedAccounts.stream()
                    .map(acc -> {
                        Map<String, Object> accMap = new HashMap<>();
                        accMap.put("id", acc.getId());
                        accMap.put("accountNumber", acc.getAccountNumber());
                        accMap.put("name", acc.getAccountType() + " Account");
                        accMap.put("type", acc.getAccountType().toLowerCase());
                        accMap.put("currency", acc.getCurrency() != null ? acc.getCurrency() : "USD");
                        // Optionally include balance (for display purposes)
                        // accMap.put("balance", acc.getBalance());
                        accMap.put("userId", acc.getUser().getId());
                        
                        // User information
                        String fullName = (acc.getUser().getFirstName() != null ? acc.getUser().getFirstName() : "") + 
                                        " " + (acc.getUser().getLastName() != null ? acc.getUser().getLastName() : "");
                        fullName = fullName.trim();
                        if (fullName.isEmpty()) {
                            fullName = acc.getUser().getUsername();
                        }
                        accMap.put("userName", fullName);
                        accMap.put("userEmail", acc.getUser().getEmail());
                        accMap.put("createdAt", acc.getCreatedAt());
                        return accMap;
                    })
                    .toList();
            
            return ResponseEntity.ok(formattedAccounts);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to search accounts: " + e.getMessage()));
        }
    }
}

