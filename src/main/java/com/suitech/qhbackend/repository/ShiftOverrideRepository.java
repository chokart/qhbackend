package com.suitech.qhbackend.repository;

import com.suitech.qhbackend.model.ShiftOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ShiftOverrideRepository extends JpaRepository<ShiftOverride, Integer> {
    List<ShiftOverride> findByDateBetween(LocalDate start, LocalDate end);
    List<ShiftOverride> findByOperatorIdAndDateBetween(Integer operatorId, LocalDate start, LocalDate end);
    Optional<ShiftOverride> findByOperatorIdAndDate(Integer operatorId, LocalDate date);
}
