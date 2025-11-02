package com.example.hack1.User.domain;

import com.example.hack1.sales.domain.Sales;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@Builder
public class User {

    @Id
    @Column(length = 50)
    private String id;

    @Size(min = 3, max = 30)
    @Column(unique = true, nullable = false)
    private String username;

    @Size(min = 8)
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private Rol role;

    private String branch;

    @Column(unique = true, nullable = false)
    @Email
    private String email;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "createdByUser", cascade = CascadeType.ALL)
    private List<Sales> sales;


    @PrePersist
    public void generateId() {
        if (this.id == null) {
            // Generar timestamp de 2 dígitos (01-99)
            long timestamp = System.currentTimeMillis() % 100;

            // Generar parte aleatoria (16 caracteres)
            String randomPart = UUID.randomUUID().toString()
                    .replace("-", "")
                    .toUpperCase()
                    .substring(0, 16);

            // Formato: u_XX + random (ejemplo: u_01A1B2C3D4E5F6G7)
            this.id = String.format("u_%02d%s", timestamp, randomPart);
        }
    }
}