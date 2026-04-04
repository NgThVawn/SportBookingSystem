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

    long countByFacility_Owner_Email(String ownerEmail);

    long countByFacility_Owner_EmailAndStatus(String ownerEmail, FieldStatus status);

    long countByFacility_IdAndStatus(Long facilityId, FieldStatus status);

    List<Field> findBySportTypeAndStatus(SportType sportType, FieldStatus status);

    boolean existsByIdAndFacility_IdAndFacility_Owner_Email(Long id, Long facilityId, String ownerEmail);

}
