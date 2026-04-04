package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.ExtraServiceRequest;
import J2EE.SportBooingSystem.entity.ExtraService;

import java.util.List;

public interface ExtraServiceService {
    List<ExtraService> findByFacility(Long facilityId);
    List<ExtraService> findAllByFacility(Long facilityId, String ownerEmail);
    ExtraService findById(Long id);
    List<ExtraService> findAllByIds(List<Long> ids);
    ExtraService save(ExtraService service);
    ExtraService create(Long facilityId, ExtraServiceRequest request, String ownerEmail);
    ExtraService update(Long serviceId, Long facilityId, ExtraServiceRequest request, String ownerEmail);
    void toggleStatus(Long serviceId, Long facilityId, boolean active, String ownerEmail);
    void delete(Long id, String ownerEmail);
}
