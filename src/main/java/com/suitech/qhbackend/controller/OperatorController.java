package com.suitech.qhbackend.controller;

import com.suitech.qhbackend.model.Operator;
import com.suitech.qhbackend.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorRepository repository;

    @GetMapping
    public List<Operator> getAllOperators() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Operator::getName))
                .collect(Collectors.toList());
    }
}
