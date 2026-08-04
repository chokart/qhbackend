package com.suitech.qhbackend.controller;

import com.suitech.qhbackend.model.Cancha;
import com.suitech.qhbackend.repository.CanchaRepository;
import com.suitech.qhbackend.service.GeotecniaReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/canchas")
@RequiredArgsConstructor
public class CanchaController {

    private final CanchaRepository repository;
    private final GeotecniaReportService reportService;

    @GetMapping
    public List<Cancha> getAllCanchas() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Cancha::getNumber))
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cancha> updateCancha(
            @PathVariable Integer id,
            @RequestBody Cancha request,
            Authentication auth
    ) {
        Cancha cancha = repository.findById(id).orElseThrow();
        
        if (request.getCurrentHeight() != null) cancha.setCurrentHeight(request.getCurrentHeight());
        if (request.getStatus() != null) cancha.setStatus(request.getStatus());
        if (request.getComment() != null) cancha.setComment(request.getComment());
        if (request.getAssignedEquipment() != null) cancha.setAssignedEquipment(request.getAssignedEquipment());
        if (request.getOperatorName() != null) cancha.setOperatorName(request.getOperatorName());
        
        cancha.setLastUpdatedBy(auth.getName());
        return ResponseEntity.ok(repository.save(cancha));
    }

    @PostMapping(value = "/upload-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPerfilPdf(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        try {
            String username = auth != null ? auth.getName() : "ADMIN";
            GeotecniaReportService.ImportReportResult result = reportService.processPerfilPdf(file, username);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error procesando el perfil PDF: " + e.getMessage());
        }
    }

    @PostMapping(value = "/upload-canchas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCanchasPdf(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        try {
            String username = auth != null ? auth.getName() : "ADMIN";
            GeotecniaReportService.ImportReportResult result = reportService.processCanchasPdf(file, username);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error procesando el reporte de canchas PDF: " + e.getMessage());
        }
    }

    @PostMapping(value = "/import-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importGeotecniaReport(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        try {
            String username = auth != null ? auth.getName() : "ADMIN";
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            if (filename.contains("perfil")) {
                return ResponseEntity.ok(reportService.processPerfilPdf(file, username));
            } else {
                return ResponseEntity.ok(reportService.processCanchasPdf(file, username));
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error procesando el reporte PDF: " + e.getMessage());
        }
    }
}
