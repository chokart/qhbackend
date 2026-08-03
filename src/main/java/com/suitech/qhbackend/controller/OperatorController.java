package com.suitech.qhbackend.controller;

import com.suitech.qhbackend.model.Group;
import com.suitech.qhbackend.model.Operator;
import com.suitech.qhbackend.repository.GroupRepository;
import com.suitech.qhbackend.repository.OperatorRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorRepository repository;
    private final GroupRepository groupRepository;

    @GetMapping
    public List<Operator> getAllOperators() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Operator::getName))
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}/group")
    public ResponseEntity<Operator> updateOperatorGroup(@PathVariable Integer id, @RequestBody ChangeGroupRequest request) {
        Operator operator = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operador no encontrado con ID: " + id));

        if (request.getGroupId() == null || request.getGroupId() <= 0) {
            operator.setGroup(null);
        } else {
            Group group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Guardia/Grupo no encontrado con ID: " + request.getGroupId()));
            operator.setGroup(group);
        }

        Operator updatedOperator = repository.save(operator);
        return ResponseEntity.ok(updatedOperator);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<Operator> updateOperatorRole(@PathVariable Integer id, @RequestBody ChangeRoleRequest request) {
        Operator operator = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operador no encontrado con ID: " + id));

        operator.setRole(request.getRole());
        Operator updatedOperator = repository.save(operator);
        return ResponseEntity.ok(updatedOperator);
    }

    @Data
    public static class ChangeGroupRequest {
        private Integer groupId;
    }

    @Data
    public static class ChangeRoleRequest {
        private String role;
    }
}
