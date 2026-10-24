package com.projectmgmt.service;

import com.projectmgmt.dto.LoginRequest;
import com.projectmgmt.dto.LoginResponse;
import com.projectmgmt.model.User;
import com.projectmgmt.repository.UserRepository;
import com.projectmgmt.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        // Same message for "no such user" and "wrong password" so attackers learn nothing
        User user = userRepository.findByEmail(request.getEmail().trim())
            .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPassword()))
            .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new LoginResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), token);
    }
}
