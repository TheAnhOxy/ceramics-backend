package com.ceramic.repository;

import com.ceramic.entity.Alert;
import com.ceramic.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByBatchId(Long batchId);
    long countByAlertType(AlertType alertType);
}
