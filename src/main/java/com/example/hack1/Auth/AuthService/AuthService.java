package com.example.hack1.Auth.AuthService;

import com.example.hack1.DTO.Request.LoginRequestDTO;
import com.example.hack1.DTO.Request.RegisterUserDTO;
import com.example.hack1.DTO.Response.LoginResponse;
import com.example.hack1.DTO.Response.UserResponseDTO;
import com.example.hack1.Security.JwtService;
import com.example.hack1.User.Repository.UserRepository;
import com.example.hack1.User.domain.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;


    @Transactional
    public UserResponseDTO register(RegisterUserDTO request){
        if (userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (userRepository.existsByUsername(request.getUsername())){
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // ¡NUNCA guardes contraseñas en texto plano!
                .role(request.getRole())
                .branch(request.getBranch())
                // Asumo que tienes un campo 'createdAt' en tu entidad User
                // Si está anotado con @CreationTimestamp, se pondrá solo.
                // Si no, añádelo: .createdAt(Instant.now())
                .build();

        // 4. Guardar el usuario en la BBDD
        User savedUser = userRepository.save(newUser);

        return modelMapper.map(savedUser, UserResponseDTO.class);
    }

    @Transactional
    public LoginResponse login(LoginRequestDTO requestDTO){

        User user = userRepository.findByEmail(requestDTO.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado (error inesperado post-autenticación)"));
        String token = jwtService.generateToken(user);

        long expiresIn = jwtService.getAccessTokenExpiration();
        return LoginResponse.builder()
                .token(token)
                .expiresIn(expiresIn)
                .role(user.getRole())
                .branch(user.getBranch())
                .build();
    }

}
