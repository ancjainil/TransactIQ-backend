package com.transactiq.backend.controller;

import com.transactiq.backend.entity.Payment;
import com.transactiq.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
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
    public ResponseEntity<List<Payment>> getAllPayments() {
        List<Payment> payments = paymentService.getAllPayments();
        return new ResponseEntity<>(payments, HttpStatus.OK);
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

