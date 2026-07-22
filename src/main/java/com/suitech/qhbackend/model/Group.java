package com.suitech.qhbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "work_group")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name; // ej: "GUARDIA 1", "GUARDIA 2"

    private String color; // ej: "#4f46e5"

    private String programType; // "PROGRAMA_1", "PROGRAMA_2", "CUSTOM"

    private LocalDate startDate; // Fecha de anclaje de inicio del ciclo

    @Column(columnDefinition = "TEXT")
    private String patternJson; // Arreglo JSON de turnos: ["D","D","D",...]
}
