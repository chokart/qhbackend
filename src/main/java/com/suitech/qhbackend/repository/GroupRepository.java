package com.suitech.qhbackend.repository;

import com.suitech.qhbackend.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Integer> {
    Optional<Group> findByName(String name);
    boolean existsByName(String name);
}
