package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.BlockedTime;
import J2EE.SportBooingSystem.entity.Field;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BlockedTimeRepository extends JpaRepository<BlockedTime, Long> {

    List<BlockedTime> findByFieldAndDateOrderByStartTime(Field field, LocalDate date);

    List<BlockedTime> findByFieldOrderByDateAscStartTimeAsc(Field field);

    @Query("""
        SELECT COUNT(bt) > 0 FROM BlockedTime bt
        WHERE bt.field = :field
          AND bt.date = :date
          AND bt.startTime < :endTime
          AND bt.endTime > :startTime
    """)
    boolean existsConflict(@Param("field") Field field,
                           @Param("date") LocalDate date,
                           @Param("startTime") LocalTime startTime,
                           @Param("endTime") LocalTime endTime);

    @Query("""
        SELECT COUNT(bt) > 0 FROM BlockedTime bt
        WHERE bt.field = :field
          AND bt.date = :date
          AND bt.startTime < :endTime
          AND bt.endTime > :startTime
          AND bt.id <> :excludeId
    """)
    boolean existsConflictExcludingId(@Param("field") Field field,
                                      @Param("date") LocalDate date,
                                      @Param("startTime") LocalTime startTime,
                                      @Param("endTime") LocalTime endTime,
                                      @Param("excludeId") Long excludeId);

    @Query("""
        SELECT COUNT(bt) > 0 FROM BlockedTime bt
        JOIN bt.field f
        JOIN f.facility fac
        JOIN fac.owner o
        WHERE bt.id = :blockedTimeId
          AND f.id = :fieldId
          AND fac.id = :facilityId
          AND o.email = :ownerEmail
    """)
    boolean ownedBy(@Param("blockedTimeId") Long blockedTimeId,
                    @Param("fieldId") Long fieldId,
                    @Param("facilityId") Long facilityId,
                    @Param("ownerEmail") String ownerEmail);

    @Query("""
        SELECT bt FROM BlockedTime bt
        JOIN FETCH bt.field f
        JOIN FETCH f.facility fac
        JOIN FETCH fac.owner o
        WHERE bt.id = :blockedTimeId
          AND f.id = :fieldId
          AND fac.id = :facilityId
          AND o.email = :ownerEmail
    """)
    Optional<BlockedTime> findOwnedForEdit(@Param("blockedTimeId") Long blockedTimeId,
                                           @Param("fieldId") Long fieldId,
                                           @Param("facilityId") Long facilityId,
                                           @Param("ownerEmail") String ownerEmail);
}
