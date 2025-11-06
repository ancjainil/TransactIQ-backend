package com.transactiq.backend.controller;

import com.transactiq.backend.entity.User;
import com.transactiq.backend.repository.UserRepository;
import com.transactiq.backend.util.RoleUtil;
import com.transactiq.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for admin-only features
 * Provides endpoints for user management, system administration, etc.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final UserRepository userRepository;
    
    /**
     * Get all users (ADMIN only)
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Only admins can access
            if (!RoleUtil.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Only admins can access user management"));
            }
            
            List<User> users = userRepository.findAll();
            
            // Format response (exclude sensitive data like passwords)
            List<Map<String, Object>> formattedUsers = users.stream()
                    .map(u -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", u.getId());
                        userMap.put("email", u.getEmail());
                        userMap.put("username", u.getUsername());
                        userMap.put("firstName", u.getFirstName());
                        userMap.put("lastName", u.getLastName());
                        userMap.put("role", u.getRole() != null ? u.getRole().name().toLowerCase() : "user");
                        userMap.put("isActive", u.getIsActive());
                        userMap.put("createdAt", u.getCreatedAt());
                        return userMap;
                    })
                    .toList();
            
            return ResponseEntity.ok(formattedUsers);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch users: " + e.getMessage()));
        }
    }
    
    /**
     * Update user role (ADMIN only)
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            Long currentUserId = SecurityUtil.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Only admins can update roles
            if (!RoleUtil.isAdmin(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Only admins can update user roles"));
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            String newRole = request.get("role");
            if (newRole == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Role is required"));
            }
            
            try {
                User.UserRole role = User.UserRole.valueOf(newRole.toUpperCase());
                user.setRole(role);
                userRepository.save(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("email", user.getEmail());
                response.put("role", user.getRole().name().toLowerCase());
                response.put("message", "User role updated successfully");
                
                return ResponseEntity.ok(response);
                
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid role. Must be: USER, CHECKER, or ADMIN"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update user role: " + e.getMessage()));
        }
    }
    
    /**
     * Get admin dashboard summary
     * Shows system-wide statistics (ADMIN only)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getAdminDashboard() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Only admins can access
            if (!RoleUtil.isAdmin(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Only admins can access admin dashboard"));
            }
            
            // Get all users count
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.findAll().stream()
                    .filter(u -> u.getIsActive() != null && u.getIsActive())
                    .count();
            
            // Get users by role
            long userCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.UserRole.USER)
                    .count();
            long checkerCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.UserRole.CHECKER)
                    .count();
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == User.UserRole.ADMIN)
                    .count();
            
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalUsers", totalUsers);
            dashboard.put("activeUsers", activeUsers);
            dashboard.put("userCount", userCount);
            dashboard.put("checkerCount", checkerCount);
            dashboard.put("adminCount", adminCount);
            
            // Note: Payment statistics would be added here if needed
            // For now, admins can use checker dashboard for payment stats
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch admin dashboard: " + e.getMessage()));
        }
    }
}

