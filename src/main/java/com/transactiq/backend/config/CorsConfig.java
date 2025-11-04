package com.transactiq.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration for the TransactIQ backend API.
 * 
 * This configuration allows cross-origin requests from the React frontend
 * running on http://localhost:5173. It enables:
 * - Allowed HTTP methods (GET, POST, PUT, DELETE, OPTIONS)
 * - Authorization and Content-Type headers
 * - Credential support for authenticated requests
 * - Exposes Authorization header to the client
 * 
 * This is a global configuration that applies to all endpoints in the application,
 * eliminating the need for per-controller CORS annotations.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .exposedHeaders("Authorization")
                .allowCredentials(true);
    }
}


