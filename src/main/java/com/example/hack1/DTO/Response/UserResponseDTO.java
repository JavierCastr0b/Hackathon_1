package com.example.hack1.DTO.Response;

import com.example.hack1.User.domain.Rol;
import lombok.Data;

import java.time.Instant;

@Data
public class UserResponseDTO {
    private String id;
    private String username;
    private String email;
    private Rol role;
    private String branch;
    private Instant createdAt;
}
