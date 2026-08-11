package com.suitech.qhbackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate reportDate;

    private Integer yearNumber;
    private Integer monthNumber;
    private Integer dayNumber;

    // Dique Principal - Producción de Arenas (TM Secas)
    private Double dpArenasGuardiaA;
    private Double dpArenasGuardiaB;
    private Double dpArenasTotalDia;
    private Double dpArenasPlanDia;
    private Double dpArenasRealAcumMes;
    private Double dpArenasPlanMes;

    // Dique Lateral - Producción de Arenas (TM Secas)
    private Double dlArenasGuardiaA;
    private Double dlArenasGuardiaB;
    private Double dlArenasTotalDia;
    private Double dlArenasPlanDia;
    private Double dlArenasRealAcumMes;
    private Double dlArenasPlanMes;

    // Producción Total
    private Double totalArenasGuardiaA;
    private Double totalArenasGuardiaB;
    private Double totalArenasDia;
    private Double totalArenasPlanDia;
    private Double totalArenasRealAcumMes;
    private Double totalArenasPlanAcumMes;

    // Niveles Operacionales
    private Double nivelPresaDpMsnm;
    private Double nivelPresaDlMsnm;
    private Double nivelAguaMsnm;
    private Double nivelLamaM;

    // Espesador de Lamas & Parámetros Hídricos
    private Integer hidrociclonesNido1;
    private Integer hidrociclonesNido2;
    private Double caudalAguaRecuperadaM3h;
    private Double ufEspesadorPct;
    private Double turbidezFnu;
    private Double caudalNeutralizacionM3h;

    // Parámetros de Cal & pH
    private Integer lechadasPreparadas;
    private Double phPuntoDilucion;
    private Double phLagunaBarcazas;
    private Double phPf4;

    // Flota Amarilla & Operación
    private Integer tractoresOperativosA;
    private Integer tractoresOperativosB;
    private Double utilizacionTractoresPct;
    private Double utilizacionCargador994kPct;
    private Double produccionVolqueteKomatsuTm;

    // Notas & Asistencia
    @Column(columnDefinition = "TEXT")
    private String asistenciaTurnoA;

    @Column(columnDefinition = "TEXT")
    private String asistenciaTurnoB;

    @Column(columnDefinition = "TEXT")
    private String novedadesEquipos;
}
