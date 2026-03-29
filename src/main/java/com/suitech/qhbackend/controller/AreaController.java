package com.suitech.qhbackend.controller;

import com.suitech.qhbackend.model.Area;
import com.suitech.qhbackend.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaRepository repository;

    @GetMapping
    public List<Area> getAllAreas() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Area> saveArea(@RequestBody Area area) {
        return ResponseEntity.ok(repository.save(area));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArea(@PathVariable Integer id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
