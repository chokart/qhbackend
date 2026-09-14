package com.suitech.qhbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceCitation {
    private String documentCode;
    private String documentName;
    private String category;
    private String documentType;
    private String revision;
    private String area;
    private String section;
    private String sectionTitle;
    private String status;
    private Integer pageNumber;
    private String excerpt;
    private Double score;
    private String intentDetected;
}
