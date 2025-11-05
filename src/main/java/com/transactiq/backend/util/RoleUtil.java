package com.transactiq.backend.util;

import com.transactiq.backend.entity.User;

/**
 * Utility class for role-based access control checks
 */
public class RoleUtil {
    
    /**
     * Check if user has checker or admin role
     * @param user The user to check
     * @return true if user is CHECKER or ADMIN, false otherwise
     */
    public static boolean canApprovePayments(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole() == User.UserRole.CHECKER || 
               user.getRole() == User.UserRole.ADMIN;
    }
    
    /**
     * Check if user is admin
     * @param user The user to check
     * @return true if user is ADMIN, false otherwise
     */
    public static boolean isAdmin(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole() == User.UserRole.ADMIN;
    }
    
    /**
     * Check if user is checker
     * @param user The user to check
     * @return true if user is CHECKER, false otherwise
     */
    public static boolean isChecker(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole() == User.UserRole.CHECKER;
    }
    
    /**
     * Check if user is regular user
     * @param user The user to check
     * @return true if user is USER, false otherwise
     */
    public static boolean isUser(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole() == User.UserRole.USER;
    }
    
    /**
     * Get formatted role name for display
     * @param user The user
     * @return Formatted role name (User, Checker, Admin)
     */
    public static String getRoleDisplayName(User user) {
        if (user == null || user.getRole() == null) {
            return "User";
        }
        return user.getRole().name().substring(0, 1) + 
               user.getRole().name().substring(1).toLowerCase();
    }
    
    /**
     * Get role in lowercase for API responses
     * @param user The user
     * @return Role in lowercase (user, checker, admin)
     */
    public static String getRoleLowercase(User user) {
        if (user == null || user.getRole() == null) {
            return "user";
        }
        return user.getRole().name().toLowerCase();
    }
}

