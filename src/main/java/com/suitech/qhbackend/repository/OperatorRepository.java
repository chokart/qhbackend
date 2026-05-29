package com.suitech.qhbackend.repository;

import com.suitech.qhbackend.model.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OperatorRepository extends JpaRepository<Operator, Integer> {
    Optional<Operator> findByName(String name);
    boolean existsByName(String name);
}
