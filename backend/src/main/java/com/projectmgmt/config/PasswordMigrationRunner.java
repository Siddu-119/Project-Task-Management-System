package com.projectmgmt.config;

import com.projectmgmt.model.User;
import com.projectmgmt.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * V1 stored plain-text passwords. On startup, any password that is not yet a
 * BCrypt hash is hashed once. Demo logins (admin123, alice123, ...) keep working.
 */
@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        for (User u : userRepository.findAll()) {
            if (!u.getPassword().startsWith("$2")) {
                u.setPassword(passwordEncoder.encode(u.getPassword()));
                userRepository.save(u);
                System.out.println("Hashed password for user: " + u.getEmail());
            }
        }
    }
}
