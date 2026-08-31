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
    private Integer pageNumber;
    private String excerpt;
    private Double score;
}
