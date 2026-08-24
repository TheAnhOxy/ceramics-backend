package com.ceramic.entity;


import com.ceramic.enums.PriorityLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_extractions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiExtraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(length = 150)
    private String pattern;

    @Column(name = "height_cm", precision = 6, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "glaze_type", length = 100)
    private String glazeType;

    @Column(name = "estimated_clay_kg", precision = 10, scale = 2)
    private BigDecimal estimatedClayKg;

    @Column(name = "firing_temp_celsius")
    private Integer firingTempCelsius;

    @Column(name = "firing_duration_hours")
    private Integer firingDurationHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false)
    private PriorityLevel priorityLevel = PriorityLevel.NORMAL;

    @Column(name = "raw_ai_json", nullable = false, columnDefinition = "JSON")
    private String rawAiJson;

    @Column(name = "ai_model", length = 50)
    private String aiModel;

    @Column(name = "confidence_note", length = 255)
    private String confidenceNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
