package com.example.hack1.User.Controller;
import com.example.hack1.DTO.Response.UserResponseDTO;
import com.example.hack1.User.Service.UsersERVICE;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UsersERVICE userService;

    /**
     * GET /api/users
     * Listar todos los usuarios
     * Solo CENTRAL
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CENTRAL')")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<UserResponseDTO> users = userService.getAllUsers(page, size);
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/users/{id}
     * Ver detalle de un usuario específico
     * Solo CENTRAL
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CENTRAL')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable String id) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * DELETE /api/users/{id}
     * Eliminar un usuario
     * Solo CENTRAL
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CENTRAL')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}











