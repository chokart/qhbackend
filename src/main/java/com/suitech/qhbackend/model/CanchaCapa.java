package com.suitech.qhbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cancha_capa")
public class CanchaCapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, unique = true)
    private Integer number;
    
    private Integer currentCapa; // Capa actual (1 a 10)
    
    @Enumerated(EnumType.STRING)
    private CanchaStatus status;
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    private String lastUpdatedBy;
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
