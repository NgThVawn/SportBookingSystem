package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.MembershipLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MembershipLevelRepository extends JpaRepository<MembershipLevel, Integer> {

    Optional<MembershipLevel> findByName(String name);

    @Query("""
        SELECT m FROM MembershipLevel m
        WHERE m.minBookings <= :count
        ORDER BY m.minBookings DESC
        LIMIT 1
    """)
    Optional<MembershipLevel> findHighestApplicableLevel(@Param("count") int completedBookings);
}
