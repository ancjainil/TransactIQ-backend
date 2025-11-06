package com.transactiq.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service for sending webhook notifications to n8n
 * Handles all notification events (payment_approved, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class N8nNotifier {
    
    private final RestTemplate restTemplate;
    
    private static final String N8N_WEBHOOK_BASE_URL = "https://ancjainil.app.n8n.cloud/webhook/webhook";
    
    /**
     * Send event notification to n8n webhook
     * 
     * @param eventType The event type (e.g., "payment_approved")
     * @param payload The payload data to send
     * @return true if notification was sent successfully, false otherwise
     */
    public boolean sendEvent(String eventType, Map<String, Object> payload) {
        String webhookUrl = N8N_WEBHOOK_BASE_URL + "/" + eventType;
        
        try {
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity with payload
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            // Send POST request
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);
            
            // Log success
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ n8n notification sent successfully: eventType={}, statusCode={}", 
                    eventType, response.getStatusCode());
                return true;
            } else {
                log.warn("⚠️ n8n notification returned non-2xx status: eventType={}, statusCode={}", 
                    eventType, response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            // Log error but don't throw exception (non-blocking)
            log.error("❌ Failed to send n8n notification: eventType={}, error={}", 
                eventType, e.getMessage(), e);
            return false;
        }
    }
}

