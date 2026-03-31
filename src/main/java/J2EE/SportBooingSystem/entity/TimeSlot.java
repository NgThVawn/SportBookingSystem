package J2EE.SportBooingSystem.entity;

import J2EE.SportBooingSystem.enums.SlotStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "time_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_slot_field_date_start",
                columnNames = {"field_id", "date", "start_time"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private Field field;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(precision = 12, scale = 2)
    private BigDecimal priceOverride;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private SlotStatus status = SlotStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by")
    private User lockedBy;

    private LocalDateTime lockedUntil;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BigDecimal getEffectivePrice() {
        if (priceOverride != null) return priceOverride;


        BigDecimal hourlyRate = field.getPricePerHour().multiply(
                BigDecimal.valueOf(field.getSlotDuration())
                        .divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP)
        );

        return hourlyRate;
    }
}
