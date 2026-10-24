package com.projectmgmt.service;

import com.projectmgmt.dto.PasswordChangeRequest;
import com.projectmgmt.model.User;
import com.projectmgmt.repository.UserRepository;
import com.projectmgmt.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllEmployees() {
        return userRepository.findByRole(User.Role.EMPLOYEE);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getMe() {
        return userRepository.findById(CurrentUser.get().id())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void changePassword(PasswordChangeRequest req) {
        User me = getMe();
        if (!passwordEncoder.matches(req.getCurrentPassword(), me.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        me.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(me);
    }
}
