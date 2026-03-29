package com.homegenie.platform.identity.repository;

import com.homegenie.platform.identity.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    List<RefreshToken> findByUserId(Long userId);
    
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);
    
    void deleteByUserId(Long userId);
    
    void deleteByToken(String token);
}
