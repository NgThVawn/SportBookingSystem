package J2EE.SportBooingSystem.entity;

import J2EE.SportBooingSystem.enums.FacilityStatus;
import J2EE.SportBooingSystem.enums.FieldStatus;
import J2EE.SportBooingSystem.enums.SportType;
import org.hibernate.annotations.BatchSize;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "facilities")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private LocalTime openTime = LocalTime.of(6, 0);

    @Column(nullable = false)
    @Builder.Default
    private LocalTime closeTime = LocalTime.of(22, 0);

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FacilityStatus status = FacilityStatus.PENDING_APPROVAL;

    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<FacilityImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private List<Field> fields = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public String getPrimaryImageUrl() {
        return images.stream()
                .filter(FacilityImage::getIsPrimary)
                .map(FacilityImage::getImageUrl)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0).getImageUrl());
    }

    public Set<SportType> getUniqueSportTypes() {
        return fields.stream()
                .filter(f -> f.getStatus() == FieldStatus.OPEN)
                .map(Field::getSportType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public BigDecimal getMinPricePerHour() {
        return fields.stream()
                .filter(f -> f.getStatus() == FieldStatus.OPEN)
                .map(Field::getPricePerHour)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    public long getOpenFieldCount() {
        return fields.stream()
                .filter(f -> f.getStatus() == FieldStatus.OPEN)
                .count();
    }
}
