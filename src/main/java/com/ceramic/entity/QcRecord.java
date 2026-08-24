package com.ceramic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "qc_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QcRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    @ToString.Exclude
    private Batch batch;

    @Column(name = "total_checked", nullable = false)
    private Integer totalChecked;

    @Column(name = "passed_count", nullable = false)
    private Integer passedCount;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount;

    @Column(name = "defect_type", length = 150)
    private String defectType;

    @Column(name = "defect_note", length = 500)
    private String defectNote;

    @Column(name = "is_critical", nullable = false)
    private Boolean isCritical = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by")
    @ToString.Exclude
    private User checkedBy;

    @CreationTimestamp
    @Column(name = "checked_at", updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime checkedAt;
}