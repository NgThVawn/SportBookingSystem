package J2EE.SportBooingSystem.entity;

import J2EE.SportBooingSystem.enums.DayType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "price_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private Field field;

    @Column(nullable = false, length = 100)
    private String name;                  // VD: "Giờ cao điểm tối"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DayType dayType;              // ALL / WEEKDAY / WEEKEND

    @Column(nullable = false)
    private LocalTime startTime;          // VD: 17:00

    @Column(nullable = false)
    private LocalTime endTime;            // VD: 22:00

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour;      // Giá mỗi giờ trong khung này

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;        // Ưu tiên cao hơn sẽ được áp dụng

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}