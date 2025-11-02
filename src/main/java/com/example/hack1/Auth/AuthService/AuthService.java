package com.example.hack1.Auth.AuthService;

import com.example.hack1.DTO.Request.LoginRequestDTO;
import com.example.hack1.DTO.Request.RegisterUserDTO;
import com.example.hack1.DTO.Response.LoginResponse;
import com.example.hack1.DTO.Response.UserResponseDTO;
import com.example.hack1.Exception.InvalidCredentialsException;
import com.example.hack1.Exception.UserNotFoundException;
import com.example.hack1.Security.JwtService;
import com.example.hack1.User.Repository.UserRepository;
import com.example.hack1.User.domain.Rol;
import com.example.hack1.User.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j  // ← Agrega esto
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ModelMapper modelMapper;

    @Transactional
    public UserResponseDTO register(RegisterUserDTO request) {
        log.info("Intentando registrar usuario: {}", request.getUsername());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }

        validateBranchByRole(request.getRole(), request.getBranch());

        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .branch(request.getRole() == Rol.BRANCH ? request.getBranch() : null)
                .createdAt(Instant.now())
                .build();

        User savedUser = userRepository.save(newUser);

        log.info("Usuario registrado exitosamente: {}", savedUser.getUsername());
        return modelMapper.map(savedUser, UserResponseDTO.class);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequestDTO requestDTO) {
        log.info("Intento de login con username/email: {}", requestDTO.getUsername());

        // Buscar usuario por email o username
        User user = userRepository.findByEmail(requestDTO.getUsername())
                .or(() -> userRepository.findByUsername(requestDTO.getUsername()))
                .orElse(null);

        if (user == null) {
            log.error("Usuario no encontrado: {}", requestDTO.getUsername());
            throw new UserNotFoundException("Usuario no encontrado");
        }

        log.info("Usuario encontrado: {}", user.getUsername());

        // Verificar contraseña
        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            log.error("Contraseña incorrecta para usuario: {}", user.getUsername());
            throw new InvalidCredentialsException("Credenciales incorrectas");
        }

        log.info("Contraseña verificada correctamente");

        // Generar token
        try {
            log.info("Cargando UserDetails para: {}", user.getEmail());
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

            log.info("Generando token JWT");
            String token = jwtService.generateToken(userDetails, user.getRole().name());

            log.info("Token generado exitosamente");
            return new LoginResponse(token, jwtService.getExpirationTime(), user.getRole(), user.getBranch());
        } catch (Exception e) {
            log.error("Error al generar token: ", e);
            throw e;
        }
    }

    private void validateBranchByRole(Rol role, String branch) {
        if (role == Rol.BRANCH) {
            if (branch == null || branch.trim().isEmpty()) {
                throw new IllegalArgumentException("El campo 'branch' es obligatorio para usuarios con rol BRANCH");
            }
        } else if (role == Rol.CENTRAL) {
            if (branch != null && !branch.trim().isEmpty()) {
                throw new IllegalArgumentException("El campo 'branch' debe ser null para usuarios con rol CENTRAL");
            }
        }
    }
}