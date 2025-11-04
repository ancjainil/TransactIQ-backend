package com.transactiq.backend.controller;

import com.transactiq.backend.entity.User;
import com.transactiq.backend.repository.UserRepository;
import com.transactiq.backend.util.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Email is required"));
            }
            
            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Password must be at least 6 characters"));
            }
            
            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Email already registered"));
            }
            
            // Generate username from email (use email prefix)
            String username = request.getEmail().split("@")[0];
            if (userRepository.existsByUsername(username)) {
                // Append number if username exists
                int counter = 1;
                String originalUsername = username;
                while (userRepository.existsByUsername(username)) {
                    username = originalUsername + counter;
                    counter++;
                }
            }
            
            // Parse name (handle "FirstName LastName" format)
            String firstName = "";
            String lastName = "";
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                String[] nameParts = request.getName().trim().split("\\s+", 2);
                firstName = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : "";
            } else {
                // If no name provided, use username as first name
                firstName = username;
            }
            
            // Create new user
            User user = new User();
            user.setUsername(username);
            user.setEmail(request.getEmail().toLowerCase().trim());
            user.setPassword(passwordEncoder.encode(request.getPassword())); // Hash password
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setIsActive(true);
            
            // Save user
            User savedUser = userRepository.save(user);
            
            // Generate JWT token
            String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail());
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", savedUser.getId());
            userData.put("email", savedUser.getEmail());
            String fullName = (savedUser.getFirstName() + " " + savedUser.getLastName()).trim();
            userData.put("name", fullName.isEmpty() ? savedUser.getUsername() : fullName);
            userData.put("username", savedUser.getUsername());
            
            response.put("user", userData);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Registration failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid credentials"));
            }
            
            User user = userOpt.get();
            
            // For now, simple password check (in production, use PasswordEncoder)
            // TODO: Implement proper password hashing during user creation
            if (!user.getPassword().equals(request.getPassword()) && 
                !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid credentials"));
            }
            
            if (Boolean.FALSE.equals(user.getIsActive())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Account is inactive"));
            }
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getEmail());
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("email", user.getEmail());
            userData.put("name", user.getFirstName() + " " + user.getLastName());
            userData.put("username", user.getUsername());
            
            response.put("user", userData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Login failed: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("message", message);
        error.put("error", "Authentication failed");
        return error;
    }
    
    @Data
    static class RegisterRequest {
        private String name;  // Full name (will be split into firstName/lastName)
        private String email;
        private String password;
    }
    
    @Data
    static class LoginRequest {
        private String email;
        private String password;
    }
}

