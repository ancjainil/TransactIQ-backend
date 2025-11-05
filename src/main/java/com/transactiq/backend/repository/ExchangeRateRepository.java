package com.transactiq.backend.repository;

import com.transactiq.backend.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    
    /**
     * Find exchange rate by currency pair
     * @param fromCurrency Source currency code
     * @param toCurrency Target currency code
     * @return Exchange rate if found
     */
    Optional<ExchangeRate> findByFromCurrencyAndToCurrencyAndIsActiveTrue(
        String fromCurrency, 
        String toCurrency
    );
    
    /**
     * Check if exchange rate exists for currency pair
     */
    boolean existsByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
    
    /**
     * Find all active exchange rates
     */
    java.util.List<ExchangeRate> findByIsActiveTrue();
}

