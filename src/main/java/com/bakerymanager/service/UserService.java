package com.bakerymanager.service;

import com.bakerymanager.entity.User;
import com.bakerymanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String LEGACY_PREFIX = "HASH_";
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
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
            if (checkPassword(password, user.getPasswordHash())) {
                migrateLegacyHashIfNeeded(user, password);
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
        validatePasswordStrength(password);
        
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
        validatePasswordStrength(newPassword);
        
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

    public boolean isManagerOrAdmin() {
        return hasRole(User.Role.ADMIN) || hasRole(User.Role.MANAGER);
    }

    public boolean isOperatorOrAbove() {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }
        return switch (currentUser.getRole()) {
            case ADMIN, MANAGER, OPERATOR, CASHIER, PRODUCTION -> true;
        };
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
    
    private String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }
    
    /**
     * Check password against hash
     */
    private boolean checkPassword(String password, String hash) {
        if (password == null || hash == null || hash.isBlank()) {
            return false;
        }

        if (isLegacyHash(hash)) {
            return legacyHash(password).equals(hash);
        }

        try {
            return passwordEncoder.matches(password, hash);
        } catch (Exception ex) {
            logger.warn("Password hash format invalid for provided user hash.");
            return false;
        }
    }

    private void migrateLegacyHashIfNeeded(User user, String rawPassword) {
        if (user == null || user.getPasswordHash() == null || !isLegacyHash(user.getPasswordHash())) {
            return;
        }
        user.setPasswordHash(hashPassword(rawPassword));
        logger.info("Migrated legacy password hash to BCrypt for user: {}", user.getUsername());
    }

    private boolean isLegacyHash(String hash) {
        return hash != null && hash.startsWith(LEGACY_PREFIX);
    }

    private String legacyHash(String password) {
        return LEGACY_PREFIX + password.hashCode();
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Parola trebuie să aibă minim 8 caractere.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("Parola trebuie să conțină litere și cifre.");
        }
    }
    
    /**
     * Initialize default admin user if no users exist
     */
    @Transactional
    public void initializeDefaultUsers() {
        if (userRepository.count() == 0) {
            createUser("admin", "admin123", "Administrator", User.Role.ADMIN);
            createUser("manager", "manager123", "Manager Operațional", User.Role.MANAGER);
            createUser("operator", "operator123", "Operator Flux", User.Role.OPERATOR);
            logger.info("Default users created");
        }
    }
}
