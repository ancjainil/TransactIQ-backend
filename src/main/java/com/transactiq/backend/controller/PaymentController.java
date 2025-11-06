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

import com.transactiq.backend.entity.Account;
import com.transactiq.backend.service.AccountService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    private final AccountService accountService;
    private final UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> request) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            // Validate required fields
            if (!request.containsKey("fromAccountId") || request.get("fromAccountId") == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "fromAccountId is required"));
            }
            
            if (!request.containsKey("toAccountId") || request.get("toAccountId") == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "toAccountId is required"));
            }
            
            if (!request.containsKey("amount") || request.get("amount") == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "amount is required"));
            }
            
            // Parse and validate fromAccountId
            Long fromAccountId;
            try {
                Object fromAccountIdObj = request.get("fromAccountId");
                if (fromAccountIdObj instanceof Number) {
                    fromAccountId = ((Number) fromAccountIdObj).longValue();
                } else {
                    fromAccountId = Long.parseLong(fromAccountIdObj.toString());
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid fromAccountId format"));
            }
            
            // Parse and validate toAccountId
            Long toAccountId;
            try {
                Object toAccountIdObj = request.get("toAccountId");
                if (toAccountIdObj instanceof Number) {
                    toAccountId = ((Number) toAccountIdObj).longValue();
                } else {
                    toAccountId = Long.parseLong(toAccountIdObj.toString());
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid toAccountId format"));
            }
            
            // Validate that fromAccount belongs to user
            Account fromAccount = accountService.getAccountById(fromAccountId)
                    .orElse(null);
            
            if (fromAccount == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                            "message", "From account not found",
                            "code", "ACCOUNT_NOT_FOUND"
                        ));
            }
            
            if (!fromAccount.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "message", "You can only create payments from your own accounts",
                            "code", "UNAUTHORIZED_ACCESS"
                        ));
            }
            
            // Validate toAccount exists
            Account toAccount = accountService.getAccountById(toAccountId)
                    .orElse(null);
            
            if (toAccount == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                            "message", "To account not found",
                            "code", "ACCOUNT_NOT_FOUND"
                        ));
            }
            
            // Parse and validate amount
            BigDecimal amount;
            try {
                Object amountObj = request.get("amount");
                if (amountObj instanceof Number) {
                    amount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
                } else {
                    amount = new BigDecimal(amountObj.toString());
                }
                if (amount.compareTo(BigDecimal.valueOf(0.01)) < 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Amount must be at least 0.01"));
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid amount format"));
            }
            
            // Get description if provided
            String description = request.containsKey("description") && request.get("description") != null
                    ? request.get("description").toString() : null;
            
            // Get transferType if provided (optional - will be auto-detected)
            Payment.TransferType transferType = null;
            if (request.containsKey("transferType") && request.get("transferType") != null) {
                try {
                    String transferTypeStr = request.get("transferType").toString().toUpperCase();
                    transferType = Payment.TransferType.valueOf(transferTypeStr);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid transferType. Must be 'internal' or 'external'"));
                }
            }
            
            // Validate currency if provided (for frontend validation)
            // Currency is auto-determined from fromAccount, but frontend may send it for validation
            if (request.containsKey("currency") && request.get("currency") != null) {
                String providedCurrency = request.get("currency").toString().toUpperCase();
                String actualCurrency = fromAccount.getCurrency();
                
                if (!providedCurrency.equalsIgnoreCase(actualCurrency)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", 
                                String.format("Currency mismatch. Expected %s (from account), but received %s", 
                                    actualCurrency, providedCurrency)));
                }
            }
            
            // Create payment entity
            // Currency is automatically determined from fromAccount currency
            // Exchange rate conversion will be handled by PaymentService
            Payment payment = new Payment();
            payment.setFromAccount(fromAccount);
            payment.setToAccount(toAccount);
            payment.setAmount(amount);
            // Currency will be set from fromAccount in PaymentService
            payment.setDescription(description);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setTransferType(transferType); // Set transfer type (can be null for auto-detection)
            
            // Create payment (service will validate balance but not transfer funds yet)
            Payment createdPayment;
            try {
                createdPayment = paymentService.createPayment(payment, userId);
            } catch (RuntimeException e) {
                // Handle specific error cases
                String errorMessage = e.getMessage();
                if (errorMessage.contains("Cannot mark as internal")) {
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body(Map.of(
                                "message", errorMessage,
                                "code", "INVALID_TRANSFER_TYPE"
                            ));
                }
                if (errorMessage.contains("Cannot mark as external")) {
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body(Map.of(
                                "message", errorMessage,
                                "code", "SELF_TRANSFER_EXTERNAL"
                            ));
                }
                if (errorMessage.contains("not found")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of(
                                "message", errorMessage,
                                "code", "ACCOUNT_NOT_FOUND"
                            ));
                }
                if (errorMessage.contains("not active")) {
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body(Map.of(
                                "message", errorMessage,
                                "code", "ACCOUNT_INACTIVE"
                            ));
                }
                if (errorMessage.contains("Insufficient balance")) {
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body(Map.of(
                                "message", errorMessage,
                                "code", "INSUFFICIENT_BALANCE"
                            ));
                }
                // Generic error
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", errorMessage));
            }
            
            // Format response
            Map<String, Object> response = new HashMap<>();
            response.put("id", createdPayment.getId());
            response.put("description", createdPayment.getDescription() != null ? createdPayment.getDescription() : "Payment");
            response.put("amount", createdPayment.getAmount());
            response.put("currency", createdPayment.getCurrency());
            response.put("transferType", createdPayment.getTransferType() != null ? 
                createdPayment.getTransferType().name().toLowerCase() : "internal");
            response.put("riskScore", createdPayment.getRiskScore() != null ? createdPayment.getRiskScore() : 0);
            response.put("riskLevel", createdPayment.getRiskLevel() != null ? 
                createdPayment.getRiskLevel().name() : "LOW");
            response.put("autoApproved", createdPayment.getAutoApproved() != null && createdPayment.getAutoApproved());
            
            // Add exchange rate information if currencies differ
            String fromCurrency = createdPayment.getFromAccount().getCurrency();
            String toCurrency = createdPayment.getToAccount().getCurrency();
            if (!fromCurrency.equalsIgnoreCase(toCurrency) && createdPayment.getExchangeRate() != null) {
                Map<String, Object> conversionInfo = new HashMap<>();
                conversionInfo.put("exchangeRate", createdPayment.getExchangeRate());
                conversionInfo.put("originalAmount", createdPayment.getAmount());
                conversionInfo.put("originalCurrency", fromCurrency);
                conversionInfo.put("convertedAmount", createdPayment.getConvertedAmount());
                conversionInfo.put("convertedCurrency", toCurrency);
                response.put("conversion", conversionInfo);
            } else {
                // Same currency - no conversion needed
                response.put("conversion", null);
            }
            
            response.put("fromAccountId", createdPayment.getFromAccount().getId());
            
            // Format fromAccount object
            Map<String, Object> fromAccountMap = new HashMap<>();
            fromAccountMap.put("id", createdPayment.getFromAccount().getId());
            fromAccountMap.put("name", createdPayment.getFromAccount().getAccountType() + " Account");
            fromAccountMap.put("currency", createdPayment.getFromAccount().getCurrency()); // Add currency
            String fromAccountNumber = createdPayment.getFromAccount().getAccountNumber();
            String maskedFromNumber = fromAccountNumber.length() > 4 
                ? "****" + fromAccountNumber.substring(fromAccountNumber.length() - 4)
                : "****" + fromAccountNumber;
            fromAccountMap.put("accountNumber", maskedFromNumber);
            response.put("fromAccount", fromAccountMap);
            
            response.put("toAccountId", createdPayment.getToAccount().getId());
            
            // Format toAccount object
            Map<String, Object> toAccountMap = new HashMap<>();
            toAccountMap.put("id", createdPayment.getToAccount().getId());
            toAccountMap.put("name", createdPayment.getToAccount().getAccountType() + " Account");
            toAccountMap.put("currency", createdPayment.getToAccount().getCurrency()); // Add currency
            String toAccountNumber = createdPayment.getToAccount().getAccountNumber();
            String maskedToNumber = toAccountNumber.length() > 4 
                ? "****" + toAccountNumber.substring(toAccountNumber.length() - 4)
                : "****" + toAccountNumber;
            toAccountMap.put("accountNumber", maskedToNumber);
            
            // Add recipient info for external transfers
            if (createdPayment.getTransferType() == Payment.TransferType.EXTERNAL) {
                toAccountMap.put("userId", createdPayment.getToAccount().getUser().getId());
                String fullName = (createdPayment.getToAccount().getUser().getFirstName() != null ? 
                    createdPayment.getToAccount().getUser().getFirstName() : "") + 
                    " " + (createdPayment.getToAccount().getUser().getLastName() != null ? 
                    createdPayment.getToAccount().getUser().getLastName() : "");
                fullName = fullName.trim();
                if (fullName.isEmpty()) {
                    fullName = createdPayment.getToAccount().getUser().getUsername();
                }
                toAccountMap.put("userName", fullName);
            }
            
            response.put("toAccount", toAccountMap);
            
            response.put("status", createdPayment.getStatus().name());
            response.put("createdAt", createdPayment.getCreatedAt());
            response.put("date", createdPayment.getCreatedAt());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to create payment: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/process")
    public ResponseEntity<Payment> processPayment(@PathVariable Long id) {
        try {
            Payment processedPayment = paymentService.processPayment(id);
            return new ResponseEntity<>(processedPayment, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getAllPayments() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            // Get current user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Payment> allPayments;
            
            // If user is admin or checker, show ALL payments
            if (RoleUtil.canApprovePayments(user)) {
                allPayments = paymentService.getAllPayments();
            } else {
                // Regular users only see their own payments
                List<Account> accounts = accountService.getActiveAccountsByUserId(userId);
                allPayments = accounts.stream()
                        .flatMap(account -> paymentService.getPaymentsByAccountId(account.getId()).stream())
                        .toList();
            }
            
            // Sort by creation date (newest first)
            allPayments = allPayments.stream()
                    .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                    .toList();
            
            // Format response to match API spec
            List<Map<String, Object>> formattedPayments = allPayments.stream()
                    .map(payment -> {
                        Map<String, Object> payMap = new HashMap<>();
                        payMap.put("id", payment.getId());
                        payMap.put("description", payment.getDescription() != null ? payment.getDescription() : "Payment");
                        payMap.put("amount", payment.getAmount());
                        payMap.put("currency", payment.getCurrency() != null ? payment.getCurrency() : "USD");
                        payMap.put("transferType", payment.getTransferType() != null ? 
                            payment.getTransferType().name().toLowerCase() : "internal");
                        payMap.put("riskScore", payment.getRiskScore() != null ? payment.getRiskScore() : 0);
                        payMap.put("riskLevel", payment.getRiskLevel() != null ? 
                            payment.getRiskLevel().name() : "LOW");
                        payMap.put("autoApproved", payment.getAutoApproved() != null && payment.getAutoApproved());
                        payMap.put("fromAccountId", payment.getFromAccount().getId());
                        
                        // Format fromAccount object
                        Map<String, Object> fromAccountMap = new HashMap<>();
                        fromAccountMap.put("id", payment.getFromAccount().getId());
                        fromAccountMap.put("name", payment.getFromAccount().getAccountType() + " Account");
                        fromAccountMap.put("currency", payment.getFromAccount().getCurrency()); // Add currency
                        String fromAccountNumber = payment.getFromAccount().getAccountNumber();
                        String maskedFromNumber = fromAccountNumber.length() > 4 
                            ? "****" + fromAccountNumber.substring(fromAccountNumber.length() - 4)
                            : "****" + fromAccountNumber;
                        fromAccountMap.put("accountNumber", maskedFromNumber);
                        payMap.put("fromAccount", fromAccountMap);
                        
                        payMap.put("toAccountId", payment.getToAccount().getId());
                        
                        // Format toAccount object
                        Map<String, Object> toAccountMap = new HashMap<>();
                        toAccountMap.put("id", payment.getToAccount().getId());
                        toAccountMap.put("name", payment.getToAccount().getAccountType() + " Account");
                        toAccountMap.put("currency", payment.getToAccount().getCurrency()); // Add currency
                        String toAccountNumber = payment.getToAccount().getAccountNumber();
                        String maskedToNumber = toAccountNumber.length() > 4 
                            ? "****" + toAccountNumber.substring(toAccountNumber.length() - 4)
                            : "****" + toAccountNumber;
                        toAccountMap.put("accountNumber", maskedToNumber);
                        
                        // Add recipient info for external transfers
                        if (payment.getTransferType() == Payment.TransferType.EXTERNAL) {
                            toAccountMap.put("userId", payment.getToAccount().getUser().getId());
                            String fullName = (payment.getToAccount().getUser().getFirstName() != null ? 
                                payment.getToAccount().getUser().getFirstName() : "") + 
                                " " + (payment.getToAccount().getUser().getLastName() != null ? 
                                payment.getToAccount().getUser().getLastName() : "");
                            fullName = fullName.trim();
                            if (fullName.isEmpty()) {
                                fullName = payment.getToAccount().getUser().getUsername();
                            }
                            toAccountMap.put("userName", fullName);
                        }
                        
                        payMap.put("toAccount", toAccountMap);
                        
                        payMap.put("date", payment.getCreatedAt());
                        payMap.put("createdAt", payment.getCreatedAt());
                        payMap.put("status", payment.getStatus().name());
                        
                        // Add currency conversion info if currencies differ
                        String fromCurr = payment.getFromAccount().getCurrency();
                        String toCurr = payment.getToAccount().getCurrency();
                        if (!fromCurr.equalsIgnoreCase(toCurr) && payment.getExchangeRate() != null) {
                            Map<String, Object> conversionInfo = new HashMap<>();
                            conversionInfo.put("exchangeRate", payment.getExchangeRate());
                            conversionInfo.put("originalAmount", payment.getAmount());
                            conversionInfo.put("originalCurrency", payment.getCurrency());
                            conversionInfo.put("convertedAmount", payment.getConvertedAmount());
                            conversionInfo.put("convertedCurrency", payment.getConvertedCurrency());
                            payMap.put("conversion", conversionInfo);
                        } else {
                            // Same currency - no conversion needed
                            payMap.put("conversion", null);
                        }
                        
                        return payMap;
                    })
                    .toList();
            
            return ResponseEntity.ok(formattedPayments);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch payments: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id)
                .map(payment -> new ResponseEntity<>(payment, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<Payment> getPaymentByTransactionId(@PathVariable String transactionId) {
        return paymentService.getPaymentByTransactionId(transactionId)
                .map(payment -> new ResponseEntity<>(payment, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Payment>> getPaymentsByAccountId(@PathVariable Long accountId) {
        List<Payment> payments = paymentService.getPaymentsByAccountId(accountId);
        return new ResponseEntity<>(payments, HttpStatus.OK);
    }
    
    @GetMapping("/account/{accountId}/outgoing")
    public ResponseEntity<List<Payment>> getOutgoingPayments(@PathVariable Long accountId) {
        List<Payment> payments = paymentService.getOutgoingPaymentsByAccountId(accountId);
        return new ResponseEntity<>(payments, HttpStatus.OK);
    }
    
    @GetMapping("/account/{accountId}/incoming")
    public ResponseEntity<List<Payment>> getIncomingPayments(@PathVariable Long accountId) {
        List<Payment> payments = paymentService.getIncomingPaymentsByAccountId(accountId);
        return new ResponseEntity<>(payments, HttpStatus.OK);
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Payment> cancelPayment(@PathVariable Long id) {
        try {
            Payment cancelledPayment = paymentService.cancelPayment(id);
            return new ResponseEntity<>(cancelledPayment, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approvePayment(@PathVariable Long id) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            // Check if user has checker or admin role
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!RoleUtil.canApprovePayments(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "message", "Only checkers and admins can approve payments",
                            "error", "Forbidden",
                            "code", "FORBIDDEN"
                        ));
            }
            
            // Approve payment
            Payment approvedPayment = paymentService.approvePayment(id, userId);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("id", approvedPayment.getId());
            response.put("status", approvedPayment.getStatus().name());
            response.put("message", "Payment approved successfully");
            response.put("approvedBy", RoleUtil.getRoleDisplayName(user));
            response.put("approvedByRole", RoleUtil.getRoleLowercase(user));
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to approve payment: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectPayment(@PathVariable Long id) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            // Check if user has checker or admin role
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!RoleUtil.canApprovePayments(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "message", "Only checkers and admins can reject payments",
                            "error", "Forbidden",
                            "code", "FORBIDDEN"
                        ));
            }
            
            // Reject payment
            Payment rejectedPayment = paymentService.rejectPayment(id, userId);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("id", rejectedPayment.getId());
            response.put("status", rejectedPayment.getStatus().name());
            response.put("message", "Payment rejected");
            response.put("rejectedBy", RoleUtil.getRoleDisplayName(user));
            response.put("rejectedByRole", RoleUtil.getRoleLowercase(user));
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to reject payment: " + e.getMessage()));
        }
    }
}

