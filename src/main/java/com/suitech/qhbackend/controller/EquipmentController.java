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
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Equipment> registerEquipment(@RequestBody Equipment equipment, Authentication auth) {
        String username = auth != null ? auth.getName() : "ADMIN";
        equipment.setLastUpdatedBy(username);
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
