package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.FacilityStatus;
import J2EE.SportBooingSystem.enums.SportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

     @EntityGraph(attributePaths = {"images"})
    List<Facility> findByOwner(User owner);

    List<Facility> findByStatusOrderByAvgRatingDesc(FacilityStatus status);

    @Query(value = """
        SELECT DISTINCT f FROM Facility f
        LEFT JOIN f.fields field
        WHERE f.status = 'OPEN'
          AND (:city IS NULL OR LOWER(f.city) LIKE LOWER(CONCAT('%', :city, '%'))
               OR LOWER(f.district) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:sport IS NULL OR field.sportType = :sport)
          AND (:name IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :name, '%'))
               OR LOWER(f.address) LIKE LOWER(CONCAT('%', :name, '%')))
        ORDER BY f.avgRating DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT f) FROM Facility f
        LEFT JOIN f.fields field
        WHERE f.status = 'OPEN'
          AND (:city IS NULL OR LOWER(f.city) LIKE LOWER(CONCAT('%', :city, '%'))
               OR LOWER(f.district) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:sport IS NULL OR field.sportType = :sport)
          AND (:name IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :name, '%'))
               OR LOWER(f.address) LIKE LOWER(CONCAT('%', :name, '%')))
        """)
    Page<Facility> searchFacilities(
            @Param("city")  String city,
            @Param("sport") SportType sport,
            @Param("name")  String name,
            Pageable pageable
    );

    long countByStatus(FacilityStatus status);
}