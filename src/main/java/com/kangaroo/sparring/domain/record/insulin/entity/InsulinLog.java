package com.kangaroo.sparring.domain.record.insulin.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.record.insulin.type.InsulinEventType;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "insulin_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InsulinLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private InsulinEventType eventType;

    @Column(name = "dose", nullable = false, precision = 6, scale = 2)
    private BigDecimal dose;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @Column(name = "insulin_type", nullable = false, length = 100)
    private String insulinType;

    @Column(name = "temp_basal_active", nullable = false)
    private boolean tempBasalActive;

    @Column(name = "temp_basal_value", nullable = false, precision = 6, scale = 2)
    private BigDecimal tempBasalValue;

    public static InsulinLog create(
            User user,
            InsulinEventType eventType,
            BigDecimal dose,
            LocalDateTime usedAt,
            String insulinType,
            boolean tempBasalActive,
            BigDecimal tempBasalValue
    ) {
        return InsulinLog.builder()
                .user(user)
                .eventType(eventType)
                .dose(dose)
                .usedAt(usedAt)
                .insulinType(insulinType)
                .tempBasalActive(tempBasalActive)
                .tempBasalValue(tempBasalValue)
                .build();
    }
}
