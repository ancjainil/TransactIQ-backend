package com.transactiq.backend.controller;

import com.transactiq.backend.entity.ExchangeRate;
import com.transactiq.backend.service.ExchangeRateService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing exchange rates
 */
@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {
    
    private final ExchangeRateService exchangeRateService;
    
    /**
     * Get exchange rate for a currency pair
     */
    @GetMapping("/{fromCurrency}/{toCurrency}")
    public ResponseEntity<?> getExchangeRate(
            @PathVariable String fromCurrency,
            @PathVariable String toCurrency) {
        try {
            BigDecimal rate = exchangeRateService.getExchangeRate(
                fromCurrency.toUpperCase(), 
                toCurrency.toUpperCase()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("fromCurrency", fromCurrency.toUpperCase());
            response.put("toCurrency", toCurrency.toUpperCase());
            response.put("rate", rate);
            response.put("description", String.format("1 %s = %s %s", 
                fromCurrency.toUpperCase(), rate, toCurrency.toUpperCase()));
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            // Exchange rate not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Exchange rate not available for " + 
                        fromCurrency.toUpperCase() + " to " + toCurrency.toUpperCase()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to get exchange rate: " + e.getMessage()));
        }
    }
    
    /**
     * Convert amount between currencies
     */
    @GetMapping("/convert")
    public ResponseEntity<?> convertAmount(
            @RequestParam BigDecimal amount,
            @RequestParam String fromCurrency,
            @RequestParam String toCurrency) {
        try {
            BigDecimal rate = exchangeRateService.getExchangeRate(
                fromCurrency.toUpperCase(), 
                toCurrency.toUpperCase()
            );
            BigDecimal convertedAmount = exchangeRateService.convertAmount(
                amount, 
                fromCurrency.toUpperCase(), 
                toCurrency.toUpperCase()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("originalAmount", amount);
            response.put("originalCurrency", fromCurrency.toUpperCase());
            response.put("convertedAmount", convertedAmount);
            response.put("convertedCurrency", toCurrency.toUpperCase());
            response.put("exchangeRate", rate);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // Invalid amount or currency code
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            // Exchange rate not found
            if (e.getMessage().contains("Exchange rate not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Exchange rate not available for " + 
                            fromCurrency.toUpperCase() + " to " + toCurrency.toUpperCase()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Conversion failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Conversion failed: " + e.getMessage()));
        }
    }
    
    /**
     * Get all active exchange rates
     * Returns formatted list matching frontend requirements
     */
    @GetMapping
    public ResponseEntity<?> getAllExchangeRates() {
        try {
            List<ExchangeRate> rates = exchangeRateService.getAllActiveRates();
            
            // Format response to match frontend requirements
            List<Map<String, Object>> formattedRates = rates.stream()
                    .map(rate -> {
                        Map<String, Object> rateMap = new HashMap<>();
                        rateMap.put("id", rate.getId());
                        rateMap.put("fromCurrency", rate.getFromCurrency());
                        rateMap.put("toCurrency", rate.getToCurrency());
                        rateMap.put("rate", rate.getRate());
                        rateMap.put("isActive", rate.getIsActive());
                        rateMap.put("createdAt", rate.getCreatedAt());
                        rateMap.put("updatedAt", rate.getUpdatedAt());
                        return rateMap;
                    })
                    .toList();
            
            return ResponseEntity.ok(formattedRates);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch exchange rates: " + e.getMessage()));
        }
    }
    
    /**
     * Create or update exchange rate (Admin only - for future implementation)
     */
    @PostMapping
    public ResponseEntity<?> createOrUpdateRate(@RequestBody ExchangeRateRequest request) {
        try {
            // Validate input
            if (request.getFromCurrency() == null || request.getToCurrency() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "fromCurrency and toCurrency are required"));
            }
            
            if (request.getRate() == null || request.getRate().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Rate must be greater than 0"));
            }
            
            ExchangeRate rate = exchangeRateService.saveExchangeRate(
                request.getFromCurrency(),
                request.getToCurrency(),
                request.getRate()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", rate.getId());
            response.put("fromCurrency", rate.getFromCurrency());
            response.put("toCurrency", rate.getToCurrency());
            response.put("rate", rate.getRate());
            response.put("message", "Exchange rate saved successfully");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Failed to save exchange rate: " + e.getMessage()));
        }
    }
    
    /**
     * Initialize default exchange rates (for development/testing)
     */
    @PostMapping("/initialize")
    public ResponseEntity<?> initializeDefaultRates() {
        try {
            exchangeRateService.initializeDefaultRates();
            return ResponseEntity.ok(Map.of("message", "Default exchange rates initialized successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to initialize rates: " + e.getMessage()));
        }
    }
    
    @Data
    static class ExchangeRateRequest {
        private String fromCurrency;
        private String toCurrency;
        private BigDecimal rate;
    }
}

