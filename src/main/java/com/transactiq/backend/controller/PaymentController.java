package com.transactiq.backend.controller;

import com.transactiq.backend.entity.Payment;
import com.transactiq.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.transactiq.backend.entity.Account;
import com.transactiq.backend.service.AccountService;
import com.transactiq.backend.util.SecurityUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    private final AccountService accountService;
    
    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        try {
            Payment createdPayment = paymentService.createPayment(payment);
            return new ResponseEntity<>(createdPayment, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
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
            
            // Get user's accounts
            List<Account> accounts = accountService.getActiveAccountsByUserId(userId);
            
            // Get all payments for user's accounts
            List<Payment> allPayments = accounts.stream()
                    .flatMap(account -> paymentService.getPaymentsByAccountId(account.getId()).stream())
                    .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                    .toList();
            
            // Format response to match API spec
            List<Map<String, Object>> formattedPayments = allPayments.stream()
                    .map(payment -> {
                        Map<String, Object> payMap = new HashMap<>();
                        payMap.put("id", payment.getId());
                        payMap.put("description", payment.getDescription() != null ? payment.getDescription() : "Payment");
                        payMap.put("amount", payment.getAmount());
                        payMap.put("date", payment.getCreatedAt());
                        payMap.put("status", payment.getStatus().name().toLowerCase());
                        return payMap;
                    })
                    .toList();
            
            return ResponseEntity.ok(formattedPayments);
            
        } catch (Exception e) {
            // Fallback to all payments if user context not available
            List<Payment> payments = paymentService.getAllPayments();
            List<Map<String, Object>> formattedPayments = payments.stream()
                    .map(payment -> {
                        Map<String, Object> payMap = new HashMap<>();
                        payMap.put("id", payment.getId());
                        payMap.put("description", payment.getDescription() != null ? payment.getDescription() : "Payment");
                        payMap.put("amount", payment.getAmount());
                        payMap.put("date", payment.getCreatedAt());
                        payMap.put("status", payment.getStatus().name().toLowerCase());
                        return payMap;
                    })
                    .toList();
            return ResponseEntity.ok(formattedPayments);
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
}

