package com.suitech.qhbackend.repository;

import com.suitech.qhbackend.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Integer> {
    boolean existsByName(String name);
}
