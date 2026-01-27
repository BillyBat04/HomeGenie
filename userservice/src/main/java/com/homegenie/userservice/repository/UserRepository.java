package com.homegenie.userservice.repository;

import com.homegenie.userservice.model.User;
import com.homegenie.userservice.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
    List<User> findByRoleAndActive(UserRole role, boolean active);
}