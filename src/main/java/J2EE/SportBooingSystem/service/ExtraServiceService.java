package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.entity.ExtraService;

import java.util.List;

public interface ExtraServiceService {
    List<ExtraService> findByFacility(Long facilityId);
    ExtraService save(ExtraService service);
    void delete(Long id, String ownerEmail);
}
