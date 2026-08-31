package com.suitech.qhbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantChatResponse {
    private String answer;
    private boolean llmAvailable;
    private String statusMessage;
    private List<SourceCitation> sources;
    private long executionTimeMs;
}
