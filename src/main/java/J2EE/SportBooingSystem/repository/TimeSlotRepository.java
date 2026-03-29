package J2EE.SportBooingSystem.repository;


import J2EE.SportBooingSystem.entity.Field;
import J2EE.SportBooingSystem.entity.TimeSlot;
import J2EE.SportBooingSystem.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByFieldAndDateOrderByStartTime(Field field, LocalDate date);

    List<TimeSlot> findByFieldAndDateAndStatusOrderByStartTime(
            Field field, LocalDate date, SlotStatus status
    );

    Optional<TimeSlot> findByFieldAndDateAndStartTime(
            Field field, LocalDate date, java.time.LocalTime startTime
    );

    boolean existsByFieldAndDateAndStartTime(
            Field field, LocalDate date, java.time.LocalTime startTime
    );

    // Release expired locks
    @Modifying
    @Query("""
        UPDATE TimeSlot ts SET ts.status = 'AVAILABLE', ts.lockedBy = null, ts.lockedUntil = null
        WHERE ts.status = 'LOCKED' AND ts.lockedUntil < :now
    """)
    int releaseExpiredLocks(@Param("now") LocalDateTime now);

    @Query("""
        SELECT ts FROM TimeSlot ts
        WHERE ts.field.facility.owner.email = :ownerEmail
          AND ts.date = :date
        ORDER BY ts.field.id, ts.startTime
    """)
    List<TimeSlot> findByOwnerEmailAndDate(
            @Param("ownerEmail") String ownerEmail,
            @Param("date") LocalDate date
    );
}
