package com.example.hack1.Auth.AuthController;

import com.example.hack1.Auth.AuthService.AuthService;
import com.example.hack1.DTO.Request.LoginRequestDTO;
import com.example.hack1.DTO.Request.RegisterUserDTO;
import com.example.hack1.DTO.Response.LoginResponse;
import com.example.hack1.DTO.Response.UserResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterUserDTO request) {
        UserResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

}
