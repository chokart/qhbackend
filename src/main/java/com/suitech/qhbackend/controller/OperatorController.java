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

import com.suitech.qhbackend.repository.ShiftOverrideRepository;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorRepository repository;
    private final GroupRepository groupRepository;
    private final ShiftOverrideRepository shiftOverrideRepository;

    @GetMapping
    public List<Operator> getAllOperators() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Operator::getName))
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<Operator> createOperator(@RequestBody CreateOperatorRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("El nombre del operador es requerido");
        }

        Operator operator = new Operator();
        operator.setCode(request.getCode() != null ? request.getCode().trim() : null);
        operator.setName(request.getName().trim());
        operator.setRole(request.getRole() != null && !request.getRole().trim().isEmpty() ? request.getRole().trim() : "OPERADOR");

        if (request.getGroupId() != null && request.getGroupId() > 0) {
            Group group = groupRepository.findById(request.getGroupId())
                    .orElse(null);
            operator.setGroup(group);
        }
        operator.setOnlyDayShift(request.getOnlyDayShift() != null ? request.getOnlyDayShift() : false);

        Operator savedOperator = repository.save(operator);
        return ResponseEntity.ok(savedOperator);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteOperator(@PathVariable Integer id) {
        Operator operator = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operador no encontrado con ID: " + id));

        shiftOverrideRepository.deleteByOperatorId(id);
        repository.delete(operator);
        return ResponseEntity.ok().build();
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

    @PutMapping("/{id}/only-day")
    public ResponseEntity<Operator> updateOperatorOnlyDay(@PathVariable Integer id, @RequestBody ChangeOnlyDayRequest request) {
        Operator operator = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operador no encontrado con ID: " + id));

        operator.setOnlyDayShift(request.getOnlyDayShift() != null ? request.getOnlyDayShift() : false);
        Operator updatedOperator = repository.save(operator);
        return ResponseEntity.ok(updatedOperator);
    }

    @Data
    public static class CreateOperatorRequest {
        private String code;
        private String name;
        private String role;
        private Integer groupId;
        private Boolean onlyDayShift;
    }

    @Data
    public static class ChangeGroupRequest {
        private Integer groupId;
    }

    @Data
    public static class ChangeRoleRequest {
        private String role;
    }

    @Data
    public static class ChangeOnlyDayRequest {
        private Boolean onlyDayShift;
    }
}
