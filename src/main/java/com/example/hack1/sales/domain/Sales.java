package com.example.hack1.sales.domain;

import com.example.hack1.User.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sales")
@Builder
public class Sales {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private Integer units;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String branch;

    @Column(nullable = false)
    private Instant soldAt;

    // ✅ Relación con User - quien creó la venta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            // Formato: s_XX + random
            long timestamp = System.currentTimeMillis() % 100;
            String randomPart = UUID.randomUUID().toString()
                    .replace("-", "")
                    .toUpperCase()
                    .substring(0, 16);
            id = String.format("s_%02d%s", timestamp, randomPart);
        }
    }
}