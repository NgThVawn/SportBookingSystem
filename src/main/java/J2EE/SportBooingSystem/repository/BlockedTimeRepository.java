package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.BlockedTime;
import J2EE.SportBooingSystem.entity.Field;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BlockedTimeRepository extends JpaRepository<BlockedTime, Long> {

    List<BlockedTime> findByFieldAndDateOrderByStartTime(Field field, LocalDate date);

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
}
