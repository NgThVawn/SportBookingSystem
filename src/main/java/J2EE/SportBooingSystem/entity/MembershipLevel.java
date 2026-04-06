package J2EE.SportBooingSystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "membership_levels")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MembershipLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(nullable = false)
    private Integer minBookings;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal discountRate;

    @Column(length = 255)
    private String description;

    @Column(length = 10)
    private String badgeColor;
}
