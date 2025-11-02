package com.example.hack1.User.Service;

import com.example.hack1.DTO.Response.UserResponseDTO;
import com.example.hack1.Exception.ResourceNotFoundException;
import com.example.hack1.Exception.UnauthorizedException;
import com.example.hack1.User.Repository.UserRepository;
import com.example.hack1.User.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersERVICE {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    /**
     * Obtener el usuario autenticado actual
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no autenticado"));
    }

    /**
     * Verificar si el usuario es CENTRAL
     */
    private void validateCentralRole() {
        User currentUser = getCurrentUser();
        if (!"CENTRAL".equals(currentUser.getRole().name())) {
            throw new UnauthorizedException("Solo usuarios CENTRAL pueden acceder a esta función");
        }
    }

    /**
     * GET /api/users - Listar todos los usuarios (solo CENTRAL)
     */
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(int page, int size) {
        validateCentralRole();

        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage = userRepository.findAll(pageable);

        log.info("Listando usuarios - Página: {}, Tamaño: {}, Total: {}",
                page, size, usersPage.getTotalElements());

        return usersPage.map(user -> modelMapper.map(user, UserResponseDTO.class));
    }

    /**
     * GET /api/users/{id} - Obtener detalle de un usuario (solo CENTRAL)
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(String id) {
        validateCentralRole();

        User user = userRepository.findById(Long.parseLong(id))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        log.info("Consultando detalle del usuario: {}", user.getUsername());

        return modelMapper.map(user, UserResponseDTO.class);
    }

    /**
     * DELETE /api/users/{id} - Eliminar un usuario (solo CENTRAL)
     */
    @Transactional
    public void deleteUser(String id) {
        validateCentralRole();

        User currentUser = getCurrentUser();

        // No permitir que se elimine a sí mismo
        if (currentUser.getId().toString().equals(id)) {
            throw new UnauthorizedException("No puedes eliminar tu propia cuenta");
        }

        User userToDelete = userRepository.findById(Long.parseLong(id))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        userRepository.deleteById(Long.parseLong(id));

        log.info("Usuario eliminado: {} por {}", userToDelete.getUsername(), currentUser.getUsername());
    }
}
