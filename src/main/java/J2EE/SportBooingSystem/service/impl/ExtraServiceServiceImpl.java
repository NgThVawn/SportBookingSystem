package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.entity.ExtraService;
import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.exception.ResourceNotFoundException;
import J2EE.SportBooingSystem.repository.ExtraServiceRepository;
import J2EE.SportBooingSystem.service.ExtraServiceService;
import J2EE.SportBooingSystem.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExtraServiceServiceImpl implements ExtraServiceService {

    private final ExtraServiceRepository extraServiceRepository;
    private final FacilityService facilityService;

    @Override
    @Transactional(readOnly = true)
    public List<ExtraService> findByFacility(Long facilityId) {
        Facility facility = facilityService.findById(facilityId);
        return extraServiceRepository.findByFacilityAndIsActiveTrue(facility);
    }

    @Override
    public ExtraService save(ExtraService service) {
        return extraServiceRepository.save(service);
    }

    @Override
    public void delete(Long id, String ownerEmail) {
        ExtraService svc = extraServiceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!svc.getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Not authorized");
        }
        svc.setIsActive(false);
    }
}
