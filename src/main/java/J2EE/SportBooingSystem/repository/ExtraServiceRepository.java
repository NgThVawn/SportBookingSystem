package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.ExtraService;
import J2EE.SportBooingSystem.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtraServiceRepository extends JpaRepository<ExtraService, Long> {
    List<ExtraService> findByFacilityAndIsActiveTrue(Facility facility);
    List<ExtraService> findByFacility(Facility facility);
}
