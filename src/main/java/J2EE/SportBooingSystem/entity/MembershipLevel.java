package J2EE.SportBooingSystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@Entity
@Table(name = "membership_levels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembershipLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;           // NONE, SILVER, GOLD, DIAMOND

    @Column(nullable = false)
    private Integer minBookings;   // số lần đặt tối thiểu để đạt hạng

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal discountRate; // 0.05 = 5%

    private String description;
    private String badgeColor;
}
