package com.suitech.qhbackend.controller;

import com.suitech.qhbackend.dto.AssistantChatRequest;
import com.suitech.qhbackend.dto.AssistantChatResponse;
import com.suitech.qhbackend.dto.AssistantStatusResponse;
import com.suitech.qhbackend.service.IsoRagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final IsoRagService ragService;

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(@RequestBody AssistantChatRequest request) {
        return ResponseEntity.ok(ragService.processChat(request));
    }

    @GetMapping("/status")
    public ResponseEntity<AssistantStatusResponse> getStatus() {
        return ResponseEntity.ok(ragService.getStatus());
    }

    @PostMapping("/index")
    public ResponseEntity<Map<String, Object>> reindexDocuments() {
        int totalChunks = ragService.indexIsoDocuments();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Indexación de documentos ISO 45001 completada exitosamente.");
        result.put("totalChunksIndexed", totalChunks);
        return ResponseEntity.ok(result);
    }
}
