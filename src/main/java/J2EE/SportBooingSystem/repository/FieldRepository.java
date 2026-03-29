package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.Field;
import J2EE.SportBooingSystem.enums.FieldStatus;
import J2EE.SportBooingSystem.enums.SportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldRepository extends JpaRepository<Field, Long> {

    List<Field> findByFacility(Facility facility);

    List<Field> findByFacilityAndStatus(Facility facility, FieldStatus status);

    List<Field> findBySportTypeAndStatus(SportType sportType, FieldStatus status);
}
