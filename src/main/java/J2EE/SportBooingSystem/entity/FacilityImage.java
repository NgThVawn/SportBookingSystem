package J2EE.SportBooingSystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "facility_images")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FacilityImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
