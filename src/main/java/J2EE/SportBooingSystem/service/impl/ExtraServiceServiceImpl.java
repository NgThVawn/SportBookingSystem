package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.ExtraServiceRequest;
import J2EE.SportBooingSystem.entity.ExtraService;
import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.exception.ForbiddenException;
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
    @Transactional(readOnly = true)
    public List<ExtraService> findAllByFacility(Long facilityId, String ownerEmail) {
        Facility facility = validateFacilityOwner(facilityId, ownerEmail);
        return extraServiceRepository.findByFacility(facility);
    }

    @Override
    @Transactional(readOnly = true)
    public ExtraService findById(Long id) {
        return extraServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtraService> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return extraServiceRepository.findByIdIn(ids);
    }

    @Override
    public ExtraService save(ExtraService service) {
        return extraServiceRepository.save(service);
    }

    @Override
    public ExtraService create(Long facilityId, ExtraServiceRequest request, String ownerEmail) {
        Facility facility = validateFacilityOwner(facilityId, ownerEmail);

        ExtraService service = ExtraService.builder()
                .facility(facility)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .unit(request.getUnit())
                .stock(request.getStock())
                .isActive(Boolean.TRUE.equals(request.getIsActive()))
                .build();
        return extraServiceRepository.save(service);
    }

    @Override
    public ExtraService update(Long serviceId, Long facilityId, ExtraServiceRequest request, String ownerEmail) {
        validateFacilityOwner(facilityId, ownerEmail);
        ExtraService service = findById(serviceId);

        if (!service.getFacility().getId().equals(facilityId)) {
            throw new IllegalArgumentException("Dịch vụ không thuộc cơ sở này");
        }

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setUnit(request.getUnit());
        service.setStock(request.getStock());
        service.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        return service;
    }

    @Override
    public void toggleStatus(Long serviceId, Long facilityId, boolean active, String ownerEmail) {
        validateFacilityOwner(facilityId, ownerEmail);
        ExtraService service = findById(serviceId);
        if (!service.getFacility().getId().equals(facilityId)) {
            throw new IllegalArgumentException("Dịch vụ không thuộc cơ sở này");
        }
        service.setIsActive(active);
    }

    @Override
    public void delete(Long id, String ownerEmail) {
        ExtraService svc = findById(id);
        if (!svc.getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new ForbiddenException("Không có quyền xóa dịch vụ này");
        }
        svc.setIsActive(false);
    }

    private Facility validateFacilityOwner(Long facilityId, String ownerEmail) {
        Facility facility = facilityService.findById(facilityId);
        if (!facility.getOwner().getEmail().equals(ownerEmail)) {
            throw new ForbiddenException("Bạn không có quyền thao tác với cơ sở này");
        }
        return facility;
    }
}
