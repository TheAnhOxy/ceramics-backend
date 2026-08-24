package com.ceramic.repository;

import com.ceramic.entity.AiExtraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiExtractionRepository extends JpaRepository<AiExtraction, Long> {
    Optional<AiExtraction> findByOrderId(Long orderId);
}
