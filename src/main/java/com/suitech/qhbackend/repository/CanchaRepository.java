package com.suitech.qhbackend.repository;

import com.suitech.qhbackend.model.Cancha;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CanchaRepository extends JpaRepository<Cancha, Integer> {
    Optional<Cancha> findByNumber(Integer number);
    boolean existsByNumber(Integer number);
}
