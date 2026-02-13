package com.bakerymanager.service;

import com.bakerymanager.entity.User;
import com.bakerymanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private User currentUser;  // Track currently logged-in user
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Authenticate user with username and password
     * For now, using simple BCrypt-like comparison (will be enhanced with Spring Security)
     */
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsernameAndIsActiveTrue(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Simple password check (will be replaced with BCrypt)
            if (checkPassword(password, user.getPasswordHash())) {
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                this.currentUser = user;
                logger.info("User authenticated: {}", username);
                return Optional.of(user);
            }
        }
        
        logger.warn("Authentication failed for user: {}", username);
        return Optional.empty();
    }
    
    /**
     * Create a new user with hashed password
     */
    public User createUser(String username, String password, String fullName, User.Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashPassword(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setIsActive(true);
        
        User savedUser = userRepository.save(user);
        logger.info("Created new user: {} with role: {}", username, role);
        return savedUser;
    }
    
    /**
     * Update user password
     */
    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setPasswordHash(hashPassword(newPassword));
        userRepository.save(user);
        logger.info("Password updated for user: {}", user.getUsername());
    }
    
    /**
     * Logout current user
     */
    public void logout() {
        if (currentUser != null) {
            logger.info("User logged out: {}", currentUser.getUsername());
            currentUser = null;
        }
    }
    
    /**
     * Get currently logged-in user
     */
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }
    
    /**
     * Check if current user has specific role
     */
    public boolean hasRole(User.Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }
    
    /**
     * Check if current user is admin
     */
    public boolean isAdmin() {
        return hasRole(User.Role.ADMIN);
    }
    
    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Deactivate user
     */
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setIsActive(false);
        userRepository.save(user);
        logger.info("User deactivated: {}", user.getUsername());
    }
    
    /**
     * Simple password hashing (placeholder for BCrypt)
     * In production, use BCryptPasswordEncoder from Spring Security
     */
    private String hashPassword(String password) {
        // For now, simple hash (will be replaced with BCrypt)
        // Using a simple hash for demonstration - NOT SECURE for production
        return "HASH_" + password.hashCode();
    }
    
    /**
     * Check password against hash
     */
    private boolean checkPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }
    
    /**
     * Initialize default admin user if no users exist
     */
    @Transactional
    public void initializeDefaultUsers() {
        if (userRepository.count() == 0) {
            createUser("admin", "admin123", "Administrator", User.Role.ADMIN);
            createUser("casier", "casier123", "Casier Principal", User.Role.CASHIER);
            logger.info("Default users created");
        }
    }
}
