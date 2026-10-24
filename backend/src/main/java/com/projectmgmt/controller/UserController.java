package com.projectmgmt.controller;

import com.projectmgmt.dto.PasswordChangeRequest;
import com.projectmgmt.model.User;
import com.projectmgmt.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/employees")
    public List<User> getAllEmployees() {
        return userService.getAllEmployees();
    }

    @GetMapping("/me")
    public User me() {
        return userService.getMe();
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest req) {
        userService.changePassword(req);
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }
}
