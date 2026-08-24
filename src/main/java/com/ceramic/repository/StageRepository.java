package com.ceramic.repository;

import com.ceramic.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StageRepository extends JpaRepository<Stage, Integer> {
    Optional<Stage> findBySequenceOrder(Integer sequenceOrder);
    Optional<Stage> findByCode(String code);
    List<Stage> findAllByOrderBySequenceOrderAsc();
}
