package com.bluestaq.notesapi.controller;

import com.bluestaq.notesapi.dto.AuthResponse;
import com.bluestaq.notesapi.dto.LoginRequest;
import com.bluestaq.notesapi.dto.RegisterRequest;
import com.bluestaq.notesapi.dto.UserResponse;
import com.bluestaq.notesapi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
