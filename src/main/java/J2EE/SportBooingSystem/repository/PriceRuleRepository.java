package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.Field;
import J2EE.SportBooingSystem.entity.PriceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalTime;
import java.util.List;

public interface PriceRuleRepository extends JpaRepository<PriceRule, Long> {

    List<PriceRule> findByFieldOrderByPriorityDesc(Field field);

    List<PriceRule> findByFieldAndIsActiveTrueOrderByPriorityDesc(Field field);

    /** Lấy tất cả rule active của field, lọc theo dayType phù hợp với ngày */
    @Query("""
        SELECT r FROM PriceRule r
        WHERE r.field = :field
          AND r.isActive = true
          AND (r.dayType = 'ALL'
               OR (r.dayType = 'WEEKDAY' AND :isWeekend = false)
               OR (r.dayType = 'WEEKEND' AND :isWeekend = true))
        ORDER BY r.priority DESC
    """)
    List<PriceRule> findActiveRulesForDay(@Param("field") Field field,
                                          @Param("isWeekend") boolean isWeekend);
}
