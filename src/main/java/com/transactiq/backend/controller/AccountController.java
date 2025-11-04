package com.transactiq.backend.controller;

import com.transactiq.backend.entity.Account;
import com.transactiq.backend.service.AccountService;
import com.transactiq.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Account> createAccount(
            @RequestBody Account account,
            @RequestParam Long userId) {
        try {
            Account createdAccount = accountService.createAccount(account, userId);
            return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
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
}

