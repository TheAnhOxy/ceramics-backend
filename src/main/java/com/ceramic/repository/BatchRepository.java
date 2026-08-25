package com.ceramic.repository;

import com.ceramic.entity.Batch;
import com.ceramic.enums.BatchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Batch b WHERE b.id = :id")
    Optional<Batch> findByIdForUpdate(@Param("id") Long id);

    Optional<Batch> findByBatchCode(String batchCode);

    List<Batch> findByStatus(BatchStatus status);

    List<Batch> findByCurrentStageId(Integer currentStageId);

    List<Batch> findByOrderId(Long orderId);
}
