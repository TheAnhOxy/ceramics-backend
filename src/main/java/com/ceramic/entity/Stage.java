package com.ceramic.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.engine.jdbc.batch.spi.Batch;

import java.util.Set;

@Entity
@Table(name = "stages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "default_duration_hours", nullable = false)
    private Integer defaultDurationHours = 24;

    @OneToMany(mappedBy = "currentStage", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Batch> batches;

    @OneToMany(mappedBy = "stage", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<BatchStageHistory> histories;
}