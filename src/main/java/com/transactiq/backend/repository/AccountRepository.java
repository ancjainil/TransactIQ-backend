package com.transactiq.backend.repository;

import com.transactiq.backend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByUserId(Long userId);
    List<Account> findByUserIdAndIsActiveTrue(Long userId);
    boolean existsByAccountNumber(String accountNumber);
    
    /**
     * Search accounts by account number, user email, user name, or account type
     * Excludes accounts belonging to the specified user
     * Returns only active accounts
     * Limits results to 10
     */
    @Query("SELECT a FROM Account a " +
           "JOIN a.user u " +
           "WHERE a.isActive = true " +
           "AND a.user.id != :excludeUserId " +
           "AND (" +
           "   LOWER(a.accountNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "   LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "   LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "   LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "   LOWER(a.accountType) LIKE LOWER(CONCAT('%', :query, '%'))" +
           ") " +
           "ORDER BY a.accountNumber ASC")
    List<Account> searchAccountsExcludingUser(@Param("query") String query, @Param("excludeUserId") Long excludeUserId);
}

