package com.suitech.qhbackend.controller;

import com.suitech.qhbackend.model.CanchaCapa;
import com.suitech.qhbackend.repository.CanchaCapaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/canchas-capas")
@RequiredArgsConstructor
public class CanchaCapaController {

    private final CanchaCapaRepository repository;

    @GetMapping
    public List<CanchaCapa> getAllCanchasCapas() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(CanchaCapa::getNumber))
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CanchaCapa> updateCanchaCapa(
            @PathVariable Integer id,
            @RequestBody CanchaCapa request,
            Authentication auth
    ) {
        CanchaCapa cancha = repository.findById(id).orElseThrow();
        
        if (request.getCurrentCapa() != null) cancha.setCurrentCapa(request.getCurrentCapa());
        if (request.getStatus() != null) cancha.setStatus(request.getStatus());
        if (request.getComment() != null) cancha.setComment(request.getComment());
        
        cancha.setLastUpdatedBy(auth.getName());
        return ResponseEntity.ok(repository.save(cancha));
    }
}
