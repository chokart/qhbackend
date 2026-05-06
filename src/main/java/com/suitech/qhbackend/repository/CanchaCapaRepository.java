package com.suitech.qhbackend.repository;

import com.suitech.qhbackend.model.CanchaCapa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CanchaCapaRepository extends JpaRepository<CanchaCapa, Integer> {
    Optional<CanchaCapa> findByNumber(Integer number);
    boolean existsByNumber(Integer number);
}
