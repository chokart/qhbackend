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

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupRepository groupRepository;
    private final OperatorRepository operatorRepository;

    @GetMapping
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestBody Group group) {
        return ResponseEntity.ok(groupRepository.save(group));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Group> updateGroup(@PathVariable Integer id, @RequestBody Group request) {
        Group group = groupRepository.findById(id).orElseThrow();
        if (request.getName() != null) group.setName(request.getName());
        if (request.getColor() != null) group.setColor(request.getColor());
        if (request.getProgramType() != null) group.setProgramType(request.getProgramType());
        if (request.getStartDate() != null) group.setStartDate(request.getStartDate());
        if (request.getPatternJson() != null) group.setPatternJson(request.getPatternJson());
        return ResponseEntity.ok(groupRepository.save(group));
    }

    @PostMapping("/{id}/assign-operators")
    public ResponseEntity<Void> assignOperators(@PathVariable Integer id, @RequestBody AssignOperatorsRequest request) {
        Group group = groupRepository.findById(id).orElseThrow();
        for (Integer opId : request.getOperatorIds()) {
            Operator op = operatorRepository.findById(opId).orElse(null);
            if (op != null) {
                op.setGroup(group);
                operatorRepository.save(op);
            }
        }
        return ResponseEntity.ok().build();
    }

    @Data
    public static class AssignOperatorsRequest {
        private List<Integer> operatorIds;
    }
}
