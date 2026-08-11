package com.suitech.qhbackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "sap_notices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SapNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemNumber;
    private String noticeNumber; // Nº Aviso SAP
    private LocalDate noticeDate;
    private String equipmentName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String locationArea;
    private String responsibleArea;
    private String reporterName;
    private String shift; // A / B
    private String guard; // G1..G4
    private String status; // Reportado / Levantado
    private LocalDate resolvedDate;
    private Integer delayDays;

    @Column(columnDefinition = "TEXT")
    private String comments;

    private Integer reportYear;
    private Integer reportMonth;
}
