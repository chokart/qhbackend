package com.suitech.qhbackend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suitech.qhbackend.model.Group;
import com.suitech.qhbackend.model.Operator;
import com.suitech.qhbackend.model.ShiftOverride;
import com.suitech.qhbackend.repository.GroupRepository;
import com.suitech.qhbackend.repository.OperatorRepository;
import com.suitech.qhbackend.repository.ShiftOverrideRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final OperatorRepository operatorRepository;
    private final GroupRepository groupRepository;
    private final ShiftOverrideRepository shiftOverrideRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/matrix")
    public ResponseEntity<ShiftMatrixResponse> getShiftMatrix(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "7") int month,
            @RequestParam(required = false) Integer groupId
    ) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int daysInMonth = firstDay.lengthOfMonth();
        LocalDate lastDay = LocalDate.of(year, month, daysInMonth);

        // Cargar operadores ordenados por Guardia (groupId) y luego por Nombre
        List<Operator> operators = operatorRepository.findAll().stream()
                .sorted(Comparator.comparing((Operator op) -> op.getGroup() != null ? op.getGroup().getId() : Integer.MAX_VALUE)
                        .thenComparing(Operator::getName))
                .collect(Collectors.toList());

        if (groupId != null) {
            operators = operators.stream()
                    .filter(op -> op.getGroup() != null && op.getGroup().getId().equals(groupId))
                    .collect(Collectors.toList());
        }

        // Cargar sobreescrituras en el rango del mes
        List<ShiftOverride> overrides = shiftOverrideRepository.findByDateBetween(firstDay, lastDay);
        Map<String, String> overrideMap = new HashMap<>(); // "opId_day" -> shiftType
        Map<String, String> overrideComments = new HashMap<>();
        for (ShiftOverride ov : overrides) {
            String key = ov.getOperator().getId() + "_" + ov.getDate().getDayOfMonth();
            overrideMap.put(key, ov.getShiftType());
            if (ov.getComment() != null) overrideComments.put(key, ov.getComment());
        }

        // Cache de patrones de grupo des-serializados
        Map<Integer, List<String>> groupPatterns = new HashMap<>();
        List<Group> allGroups = groupRepository.findAll();
        for (Group g : allGroups) {
            if (g.getPatternJson() != null && !g.getPatternJson().isEmpty()) {
                try {
                    List<String> pattern = objectMapper.readValue(g.getPatternJson(), new TypeReference<List<String>>() {});
                    groupPatterns.put(g.getId(), pattern);
                } catch (Exception e) {
                    groupPatterns.put(g.getId(), Collections.emptyList());
                }
            }
        }

        List<OperatorShiftData> resultOperators = new ArrayList<>();

        for (Operator op : operators) {
            Map<Integer, String> shifts = new HashMap<>();
            Map<Integer, String> baseShifts = new HashMap<>();
            Map<Integer, Boolean> isOverrideMap = new HashMap<>();
            Map<Integer, String> commentMap = new HashMap<>();

            Group group = op.getGroup();
            List<String> pattern = group != null ? groupPatterns.get(group.getId()) : null;
            LocalDate groupStart = group != null ? group.getStartDate() : null;

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate currentDate = LocalDate.of(year, month, day);
                String ovKey = op.getId() + "_" + day;

                String baseShift = "L";
                if (group != null && groupStart != null && pattern != null && !pattern.isEmpty()) {
                    long daysDiff;
                    if (!currentDate.isBefore(groupStart)) {
                        daysDiff = ChronoUnit.DAYS.between(groupStart, currentDate);
                    } else {
                        long diffBefore = ChronoUnit.DAYS.between(currentDate, groupStart);
                        long mod = diffBefore % pattern.size();
                        daysDiff = (pattern.size() - mod) % pattern.size();
                    }
                    int patternIdx = (int) (daysDiff % pattern.size());
                    baseShift = pattern.get(patternIdx);
                }
                baseShifts.put(day, baseShift);

                if (overrideMap.containsKey(ovKey)) {
                    shifts.put(day, overrideMap.get(ovKey));
                    isOverrideMap.put(day, true);
                    if (overrideComments.containsKey(ovKey)) {
                        commentMap.put(day, overrideComments.get(ovKey));
                    }
                } else {
                    shifts.put(day, baseShift);
                    isOverrideMap.put(day, false);
                }
            }

            OperatorShiftData data = new OperatorShiftData();
            data.setOperatorId(op.getId());
            data.setCode(op.getCode());
            data.setName(op.getName());
            data.setGroupId(group != null ? group.getId() : null);
            data.setGroupName(group != null ? group.getName() : "Sin Guardia");
            data.setGroupColor(group != null ? group.getColor() : "#94a3b8");
            data.setShifts(shifts);
            data.setBaseShifts(baseShifts);
            data.setIsOverride(isOverrideMap);
            data.setComments(commentMap);
            resultOperators.add(data);
        }

        ShiftMatrixResponse response = new ShiftMatrixResponse();
        response.setYear(year);
        response.setMonth(month);
        response.setDaysInMonth(daysInMonth);
        response.setOperators(resultOperators);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/override")
    public ResponseEntity<Void> createOverride(@RequestBody OverrideRequest request) {
        Operator operator = operatorRepository.findById(request.getOperatorId()).orElseThrow();
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = request.getEndDate() != null && !request.getEndDate().isEmpty() 
                ? LocalDate.parse(request.getEndDate()) 
                : startDate;

        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            Optional<ShiftOverride> existingOpt = shiftOverrideRepository.findByOperatorIdAndDate(operator.getId(), curr);
            ShiftOverride override;
            if (existingOpt.isPresent()) {
                override = existingOpt.get();
            } else {
                override = new ShiftOverride();
                override.setOperator(operator);
                override.setDate(curr);
            }
            override.setShiftType(request.getShiftType());
            override.setComment(request.getComment());
            shiftOverrideRepository.save(override);
            curr = curr.plusDays(1);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/override")
    public ResponseEntity<Void> deleteOverride(
            @RequestParam Integer operatorId,
            @RequestParam String date
    ) {
        LocalDate localDate = LocalDate.parse(date);
        Optional<ShiftOverride> existingOpt = shiftOverrideRepository.findByOperatorIdAndDate(operatorId, localDate);
        existingOpt.ifPresent(shiftOverrideRepository::delete);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class ShiftMatrixResponse {
        private int year;
        private int month;
        private int daysInMonth;
        private List<OperatorShiftData> operators;
    }

    @Data
    public static class OperatorShiftData {
        private Integer operatorId;
        private String code;
        private String name;
        private Integer groupId;
        private String groupName;
        private String groupColor;
        private Map<Integer, String> shifts;
        private Map<Integer, String> baseShifts;
        private Map<Integer, Boolean> isOverride;
        private Map<Integer, String> comments;
    }

    @Data
    public static class OverrideRequest {
        private Integer operatorId;
        private String startDate; // "YYYY-MM-DD"
        private String endDate;   // "YYYY-MM-DD" (opcional para rangos de vacaciones)
        private String shiftType; // "D", "N", "L", "V"
        private String comment;
    }
}
