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
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

     @Query("""
          SELECT f FROM Facility f
          LEFT JOIN FETCH f.owner
          LEFT JOIN FETCH f.images
          WHERE f.id = :id
     """)
     Optional<Facility> findByIdWithOwner(@Param("id") Long id);

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
          AND (:isFavFilter = false OR EXISTS (SELECT 1 FROM Favorite fav WHERE fav.facility = f AND fav.user.id = :userId))
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
          AND (:isFavFilter = false OR EXISTS (SELECT 1 FROM Favorite fav WHERE fav.facility = f AND fav.user.id = :userId))
        """)
    Page<Facility> searchFacilities(
            @Param("city")  String city,
            @Param("sport") SportType sport,
            @Param("name")  String name,
            @Param("isFavFilter") boolean isFavFilter,
            @Param("userId") Long userId,
            Pageable pageable
    );

    long countByStatus(FacilityStatus status);
}