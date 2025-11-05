package com.transactiq.backend.config;

import com.transactiq.backend.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes default exchange rates on application startup
 * This ensures exchange rates are available for currency conversion
 */
@Component
@RequiredArgsConstructor
public class ExchangeRateInitializer implements CommandLineRunner {
    
    private final ExchangeRateService exchangeRateService;
    
    @Override
    public void run(String... args) {
        try {
            exchangeRateService.initializeDefaultRates();
            System.out.println("Default exchange rates initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize exchange rates: " + e.getMessage());
            // Don't fail startup if rates already exist
        }
    }
}

