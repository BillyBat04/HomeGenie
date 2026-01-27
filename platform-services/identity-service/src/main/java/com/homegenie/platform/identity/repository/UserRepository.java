package com.homegenie.platform.identity.repository;

import com.homegenie.platform.identity.model.User;
import com.homegenie.platform.identity.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * User Repository
 * ✅ IDENTICAL to User Service UserRepository
 * Queries shared database table: users
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<User> findByRole(UserRole role);
    
    List<User> findByRoleAndActive(UserRole role, boolean active);
}
