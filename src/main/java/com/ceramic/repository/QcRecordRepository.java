package com.ceramic.repository;

import com.ceramic.entity.QcRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QcRecordRepository extends JpaRepository<QcRecord, Long> {
    List<QcRecord> findByBatchId(Long batchId);
}
