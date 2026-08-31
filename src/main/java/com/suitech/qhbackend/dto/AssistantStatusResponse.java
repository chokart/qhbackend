package com.suitech.qhbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantStatusResponse {
    private boolean llmAvailable;
    private String llmProvider;
    private String isoFolderPath;
    private long totalChunksIndexed;
    private long totalDocumentsIndexed;
    private Map<String, Long> chunksByCategory;
    private String lastIndexedAt;
}
