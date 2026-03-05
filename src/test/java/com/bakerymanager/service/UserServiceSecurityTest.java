package com.bakerymanager.service;

import com.bakerymanager.entity.User;
import com.bakerymanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void shouldRejectWeakPasswordWhenCreatingUser() {
        UserService service = new UserService(userRepository);
        when(userRepository.existsByUsername("weak")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            service.createUser("weak", "123", "Weak User", User.Role.OPERATOR)
        );
    }

    @Test
    void shouldMigrateLegacyHashToBcryptOnSuccessfulLogin() {
        UserService service = new UserService(userRepository);

        User user = new User();
        user.setUsername("legacy");
        user.setPasswordHash("HASH_" + "admin123".hashCode());
        user.setRole(User.Role.ADMIN);
        user.setIsActive(true);

        when(userRepository.findByUsernameAndIsActiveTrue("legacy")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> auth = service.authenticate("legacy", "admin123");

        assertTrue(auth.isPresent());
        assertFalse(auth.get().getPasswordHash().startsWith("HASH_"));
    }
}
