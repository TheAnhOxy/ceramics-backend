package com.ceramic.repository;

import com.ceramic.entity.BatchStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchStageHistoryRepository extends JpaRepository<BatchStageHistory, Long> {
    Optional<BatchStageHistory> findByBatchIdAndStageId(Long batchId, Integer stageId);
    List<BatchStageHistory> findByBatchIdOrderByStageSequenceOrderAsc(Long batchId);
}
