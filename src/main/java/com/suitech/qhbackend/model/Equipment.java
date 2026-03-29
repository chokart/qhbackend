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
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false)
    private String name;
    
    private Double latitude;
    private Double longitude;
    
    private String color; // Ej: "#ff0000" para rojo
    private String currentArea; // Nuevo campo para el área donde está
    
    @Enumerated(EnumType.STRING)
    private EquipmentStatus status;
    
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
