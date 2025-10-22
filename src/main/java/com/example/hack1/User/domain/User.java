package com.example.hack1.User.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 3, max = 30)
    @Column(unique = true, nullable = false) // Username también debe ser único
    private String username;

    @Size(min = 8)
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING) // 1. Corregido
    @Column(nullable = false)
    @NotNull
    private Rol role;

    private String branch;

    @Column(unique = true, nullable = false)
    @Email
    private String email;

    @CreationTimestamp // 5. Añadido
    @Column(nullable = false, updatable = false)
    private Instant createdAt;


    // --- MÉTODOS DE UserDetails CORREGIDOS ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 3. Corregido
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        // 2. Corregido y añadido
        // Se usa email para el login, por eso se devuelve email
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 4. Corregido
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 4. Corregido
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 4. Corregido
    }

    @Override
    public boolean isEnabled() {
        return true; // 4. Corregido
    }
}
