package com.suitech.qhbackend.controller;

import com.suitech.qhbackend.model.Equipment;
import com.suitech.qhbackend.repository.EquipmentRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentRepository repository;

    @GetMapping
    public List<Equipment> getAllEquipment() {
        List<Equipment> list = repository.findAll();
        boolean updatedAny = false;
        for (Equipment eq : list) {
            if (eq.getShortCode() == null || eq.getShortCode().trim().isEmpty()) {
                eq.setShortCode(generateShortCode(eq.getName()));
                updatedAny = true;
            }
            if (eq.getEquipmentType() == null || eq.getEquipmentType().trim().isEmpty()) {
                eq.setEquipmentType(inferEquipmentType(eq.getName()));
                updatedAny = true;
            }
        }
        if (updatedAny) {
            repository.saveAll(list);
        }
        return list;
    }

    @PostMapping
    public ResponseEntity<Equipment> registerEquipment(@RequestBody Equipment equipment, Authentication auth) {
        String username = auth != null ? auth.getName() : "ADMIN";
        equipment.setLastUpdatedBy(username);
        if (equipment.getShortCode() == null || equipment.getShortCode().trim().isEmpty()) {
            equipment.setShortCode(generateShortCode(equipment.getName()));
        }
        if (equipment.getEquipmentType() == null || equipment.getEquipmentType().trim().isEmpty()) {
            equipment.setEquipmentType(inferEquipmentType(equipment.getName()));
        }
        return ResponseEntity.ok(repository.save(equipment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Equipment> updateEquipment(
            @PathVariable Integer id,
            @RequestBody Equipment request,
            Authentication auth
    ) {
        Equipment equipment = repository.findById(id).orElseThrow();
        if (request.getName() != null) equipment.setName(request.getName());
        if (request.getShortCode() != null) equipment.setShortCode(request.getShortCode());
        if (request.getDescription() != null) equipment.setDescription(request.getDescription());
        if (request.getPlate() != null) equipment.setPlate(request.getPlate());
        if (request.getSpccCode() != null) equipment.setSpccCode(request.getSpccCode());
        if (request.getEquipmentType() != null) equipment.setEquipmentType(request.getEquipmentType());
        if (request.getStatus() != null) equipment.setStatus(request.getStatus());
        if (request.getComment() != null) equipment.setComment(request.getComment());
        if (request.getColor() != null) equipment.setColor(request.getColor());

        String username = auth != null ? auth.getName() : "ADMIN";
        equipment.setLastUpdatedBy(username);
        return ResponseEntity.ok(repository.save(equipment));
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<Equipment> updateLocation(
            @PathVariable Integer id,
            @RequestBody LocationRequest request,
            Authentication auth
    ) {
        Equipment equipment = repository.findById(id).orElseThrow();
        equipment.setLatitude(request.getLatitude());
        equipment.setLongitude(request.getLongitude());
        equipment.setCurrentArea(request.getCurrentArea()); // Guardamos en qué área está
        equipment.setLastUpdatedBy(auth.getName());
        return ResponseEntity.ok(repository.save(equipment));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Equipment> updateStatus(
            @PathVariable Integer id,
            @RequestBody StatusUpdateRequest request,
            Authentication auth
    ) {
        Equipment equipment = repository.findById(id).orElseThrow();
        if (request.getStatus() != null) equipment.setStatus(request.getStatus());
        if (request.getComment() != null) equipment.setComment(request.getComment());
        equipment.setLastUpdatedBy(auth.getName());
        return ResponseEntity.ok(repository.save(equipment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Integer id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public static String generateShortCode(String name) {
        if (name == null || name.trim().isEmpty()) return "EQ";
        String n = name.trim();
        if (n.startsWith("BATERIA")) return "B" + n.substring(7).trim();
        if (n.startsWith("NIDO")) return "N" + n.substring(4).trim();
        if (n.startsWith("Rodillo #")) return "R" + n.replace("Rodillo #", "").trim();
        if (n.startsWith("Volquete #")) return "V" + n.replace("Volquete #", "").trim();
        if (n.toLowerCase().startsWith("cisterna")) return "CIS" + n.replaceAll("(?i)cisterna", "").replace("#", "").trim();
        if (n.toLowerCase().startsWith("tracto")) return "TR" + n.replaceAll("(?i)tracto", "").replace("#", "").trim();
        if (n.toLowerCase().startsWith("retroexcavadora")) return "RT" + n.replaceAll("(?i)retroexcavadora", "").trim();
        if (n.toLowerCase().startsWith("motoniveladora")) return "MN" + n.replaceAll("(?i)motoniveladora", "").trim();
        if (n.toLowerCase().startsWith("cargador")) return "CF" + n.replaceAll("(?i)cargador", "").trim();
        if (n.toLowerCase().startsWith("exc.")) return "EXC-" + n.replace("Exc.", "").trim();
        return n;
    }

    public static String inferEquipmentType(String name) {
        if (name == null) return "OTROS";
        String lower = name.toLowerCase();
        if (name.startsWith("BATERIA") || name.startsWith("NIDO")) return "HIDROCICLON";
        if (name.startsWith("D8") || name.startsWith("D9") || name.startsWith("D10") || lower.contains("tractor")) return "TRACTOR";
        if (name.contains("Exc.") || lower.contains("excavadora")) return "EXCAVADORA";
        if (lower.contains("cisterna") || lower.contains("agua")) return "CISTERNA";
        if (lower.contains("tracto")) return "TRACTO";
        if (lower.contains("grúa") || lower.contains("grua")) return "CAMION_GRUA";
        if (lower.contains("camabaja")) return "CAMABAJA";
        if (lower.contains("cargador")) return "CARGADOR";
        if (lower.contains("volquete")) return "VOLQUETE";
        if (lower.contains("rodillo")) return "RODILLO";
        if (lower.contains("motoniveladora")) return "MOTONIVELADORA";
        return "OTROS";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateRequest {
        private com.suitech.qhbackend.model.EquipmentStatus status;
        private String comment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationRequest {
        private Double latitude;
        private Double longitude;
        private String currentArea; // Nuevo campo en la petición
    }
}
