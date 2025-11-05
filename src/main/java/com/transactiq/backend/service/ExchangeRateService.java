package com.transactiq.backend.service;

import com.transactiq.backend.entity.ExchangeRate;
import com.transactiq.backend.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing exchange rates and currency conversions
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExchangeRateService {
    
    private final ExchangeRateRepository exchangeRateRepository;
    
    /**
     * Get exchange rate for currency pair
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @return Exchange rate (1 fromCurrency = rate toCurrency)
     */
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        // Validate currency codes
        String[] supportedCurrencies = {"USD", "CAD", "EUR"};
        String fromUpper = fromCurrency.toUpperCase();
        String toUpper = toCurrency.toUpperCase();
        
        boolean fromValid = false;
        boolean toValid = false;
        for (String supported : supportedCurrencies) {
            if (supported.equals(fromUpper)) fromValid = true;
            if (supported.equals(toUpper)) toValid = true;
        }
        
        if (!fromValid) {
            throw new IllegalArgumentException(
                String.format("Invalid currency code: %s. Supported: USD, CAD, EUR", fromCurrency)
            );
        }
        if (!toValid) {
            throw new IllegalArgumentException(
                String.format("Invalid currency code: %s. Supported: USD, CAD, EUR", toCurrency)
            );
        }
        
        // Same currency, no conversion needed
        if (fromUpper.equals(toUpper)) {
            return BigDecimal.ONE;
        }
        
        // Try direct rate
        Optional<ExchangeRate> directRate = exchangeRateRepository
            .findByFromCurrencyAndToCurrencyAndIsActiveTrue(fromCurrency.toUpperCase(), toCurrency.toUpperCase());
        
        if (directRate.isPresent()) {
            return directRate.get().getRate();
        }
        
        // Try reverse rate
        Optional<ExchangeRate> reverseRate = exchangeRateRepository
            .findByFromCurrencyAndToCurrencyAndIsActiveTrue(toCurrency.toUpperCase(), fromCurrency.toUpperCase());
        
        if (reverseRate.isPresent()) {
            return reverseRate.get().getReverseRate();
        }
        
        // Try USD as intermediate currency (USD is often the base currency)
        if (!fromUpper.equals("USD") && !toUpper.equals("USD")) {
            Optional<ExchangeRate> fromToUsd = exchangeRateRepository
                .findByFromCurrencyAndToCurrencyAndIsActiveTrue(fromUpper, "USD");
            Optional<ExchangeRate> usdToTo = exchangeRateRepository
                .findByFromCurrencyAndToCurrencyAndIsActiveTrue("USD", toUpper);
            
            if (fromToUsd.isPresent() && usdToTo.isPresent()) {
                // Convert: fromCurrency -> USD -> toCurrency
                BigDecimal usdRate = fromToUsd.get().getRate(); // 1 fromCurrency = usdRate USD
                BigDecimal toRate = usdToTo.get().getRate(); // 1 USD = toRate toCurrency
                return usdRate.multiply(toRate).setScale(6, RoundingMode.HALF_UP);
            }
        }
        
        throw new RuntimeException(
            String.format("Exchange rate not found for %s to %s", fromUpper, toUpper)
        );
    }
    
    /**
     * Convert amount from one currency to another
     * @param amount Amount to convert
     * @param fromCurrency Source currency
     * @param toCurrency Target currency
     * @return Converted amount
     */
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must be provided");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Create or update exchange rate
     */
    public ExchangeRate saveExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate) {
        Optional<ExchangeRate> existing = exchangeRateRepository
            .findByFromCurrencyAndToCurrencyAndIsActiveTrue(fromCurrency.toUpperCase(), toCurrency.toUpperCase());
        
        if (existing.isPresent()) {
            ExchangeRate exchangeRate = existing.get();
            exchangeRate.setRate(rate);
            return exchangeRateRepository.save(exchangeRate);
        } else {
            ExchangeRate exchangeRate = new ExchangeRate();
            exchangeRate.setFromCurrency(fromCurrency.toUpperCase());
            exchangeRate.setToCurrency(toCurrency.toUpperCase());
            exchangeRate.setRate(rate);
            exchangeRate.setIsActive(true);
            return exchangeRateRepository.save(exchangeRate);
        }
    }
    
    /**
     * Get all active exchange rates
     */
    public List<ExchangeRate> getAllActiveRates() {
        return exchangeRateRepository.findByIsActiveTrue();
    }
    
    /**
     * Initialize default exchange rates (for development/testing)
     */
    public void initializeDefaultRates() {
        // USD to CAD (example: 1 USD = 1.35 CAD)
        if (!exchangeRateRepository.existsByFromCurrencyAndToCurrency("USD", "CAD")) {
            saveExchangeRate("USD", "CAD", new BigDecimal("1.350000"));
        }
        
        // USD to EUR (example: 1 USD = 0.92 EUR)
        if (!exchangeRateRepository.existsByFromCurrencyAndToCurrency("USD", "EUR")) {
            saveExchangeRate("USD", "EUR", new BigDecimal("0.920000"));
        }
        
        // CAD to USD (reverse of USD to CAD)
        if (!exchangeRateRepository.existsByFromCurrencyAndToCurrency("CAD", "USD")) {
            saveExchangeRate("CAD", "USD", new BigDecimal("0.740741")); // 1/1.35
        }
        
        // EUR to USD (reverse of USD to EUR)
        if (!exchangeRateRepository.existsByFromCurrencyAndToCurrency("EUR", "USD")) {
            saveExchangeRate("EUR", "USD", new BigDecimal("1.086957")); // 1/0.92
        }
        
        // CAD to EUR (via USD: CAD -> USD -> EUR)
        if (!exchangeRateRepository.existsByFromCurrencyAndToCurrency("CAD", "EUR")) {
            BigDecimal cadToUsd = getExchangeRate("CAD", "USD");
            BigDecimal usdToEur = getExchangeRate("USD", "EUR");
            saveExchangeRate("CAD", "EUR", cadToUsd.multiply(usdToEur).setScale(6, RoundingMode.HALF_UP));
        }
        
        // EUR to CAD (via USD: EUR -> USD -> CAD)
        if (!exchangeRateRepository.existsByFromCurrencyAndToCurrency("EUR", "CAD")) {
            BigDecimal eurToUsd = getExchangeRate("EUR", "USD");
            BigDecimal usdToCad = getExchangeRate("USD", "CAD");
            saveExchangeRate("EUR", "CAD", eurToUsd.multiply(usdToCad).setScale(6, RoundingMode.HALF_UP));
        }
    }
}

