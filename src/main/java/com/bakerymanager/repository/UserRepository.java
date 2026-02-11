package com.bakerymanager.repository;

import com.bakerymanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByUsernameAndIsActiveTrue(String username);
    
    boolean existsByUsername(String username);
}
