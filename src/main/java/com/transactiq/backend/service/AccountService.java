package com.transactiq.backend.service;

import com.transactiq.backend.entity.Account;
import com.transactiq.backend.entity.User;
import com.transactiq.backend.repository.AccountRepository;
import com.transactiq.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    
    public Account createAccount(Account account, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Generate unique account number if not provided
        if (account.getAccountNumber() == null || account.getAccountNumber().isEmpty()) {
            account.setAccountNumber(generateAccountNumber());
        }
        
        // Check if account number already exists
        if (accountRepository.existsByAccountNumber(account.getAccountNumber())) {
            throw new RuntimeException("Account number already exists: " + account.getAccountNumber());
        }
        
        account.setUser(user);
        return accountRepository.save(account);
    }
    
    public List<Account> getAllAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }
    
    public List<Account> getActiveAccountsByUserId(Long userId) {
        return accountRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }
    
    public Optional<Account> getAccountByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }
    
    /**
     * Search accounts excluding those belonging to the specified user
     * Used for external account search
     */
    public List<Account> searchAccountsExcludingUser(String query, Long excludeUserId) {
        return accountRepository.searchAccountsExcludingUser(query, excludeUserId);
    }
    
    public Account updateAccount(Long id, Account accountDetails) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
        
        if (accountDetails.getAccountType() != null) {
            account.setAccountType(accountDetails.getAccountType());
        }
        if (accountDetails.getCurrency() != null) {
            account.setCurrency(accountDetails.getCurrency());
        }
        if (accountDetails.getIsActive() != null) {
            account.setIsActive(accountDetails.getIsActive());
        }
        
        return accountRepository.save(account);
    }
    
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
        account.setIsActive(false);
        accountRepository.save(account);
    }
    
    public Account updateBalance(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));
        
        BigDecimal newBalance = account.getBalance().add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        
        account.setBalance(newBalance);
        return accountRepository.save(account);
    }
    
    private String generateAccountNumber() {
        return "ACC" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

