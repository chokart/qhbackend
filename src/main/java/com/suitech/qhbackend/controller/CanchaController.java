package com.suitech.qhbackend.controller;

import com.suitech.qhbackend.model.Cancha;
import com.suitech.qhbackend.repository.CanchaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/canchas")
@RequiredArgsConstructor
public class CanchaController {

    private final CanchaRepository repository;

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
        
        cancha.setLastUpdatedBy(auth.getName());
        return ResponseEntity.ok(repository.save(cancha));
    }
}
