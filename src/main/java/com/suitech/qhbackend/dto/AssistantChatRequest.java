package com.suitech.qhbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantChatRequest {
    private String message;
    private String categoryFilter;
    private java.util.List<HistoryItem> history;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private String sender;
        private String text;
    }
}
