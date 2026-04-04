package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.FieldRequest;
import J2EE.SportBooingSystem.entity.ExtraService;
import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.Field;
import J2EE.SportBooingSystem.enums.FieldStatus;
import J2EE.SportBooingSystem.enums.SportType;
import J2EE.SportBooingSystem.exception.ResourceNotFoundException;
import J2EE.SportBooingSystem.repository.ExtraServiceRepository;
import J2EE.SportBooingSystem.repository.FieldRepository;
import J2EE.SportBooingSystem.service.FacilityService;
import J2EE.SportBooingSystem.service.FieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class FieldServiceImpl implements FieldService {

    private final FieldRepository fieldRepository;
    private final ExtraServiceRepository extraServiceRepository;
    private final FacilityService facilityService;

    @Override
    public Field create(Long facilityId, FieldRequest req, String ownerEmail) {
        Facility facility = facilityService.findById(facilityId);
        if (!facility.getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Not authorized");
        }
        Field field = mapToEntity(new Field(), req);
        field.setFacility(facility);
        // Set mặc định khi tạo mới là OPEN
        if (field.getStatus() == null) field.setStatus(FieldStatus.OPEN);
        Field savedField = fieldRepository.save(field);
        seedSportSpecificService(savedField.getFacility(), savedField.getSportType());
        return savedField;
    }

    @Override
    public Field update(Long fieldId, FieldRequest req, String ownerEmail) {
        Field field = findById(fieldId);
        if (!field.getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Not authorized");
        }
        Field updatedField = fieldRepository.save(mapToEntity(field, req));
        seedSportSpecificService(updatedField.getFacility(), updatedField.getSportType());
        return updatedField;
    }

    @Override
    @Transactional(readOnly = true)
    public Field findById(Long id) {
        return fieldRepository.findByIdWithFacility(id)
            .orElseThrow(() -> new ResourceNotFoundException("Field not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Field> findByFacility(Long facilityId) {
        Facility facility = facilityService.findById(facilityId);
        return fieldRepository.findByFacility(facility);
    }

    @Override
    public void changeStatus(Long fieldId, String status, String ownerEmail) {
        Field field = findById(fieldId);
        if (!field.getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Not authorized");
        }
        try {
            field.setStatus(FieldStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }
    }

    
    @Override
    public void delete(Long fieldId, String ownerEmail) {
        Field field = findById(fieldId);

        if (!field.getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Not authorized to delete this field");
        }
        
        fieldRepository.delete(field);
    }

    private Field mapToEntity(Field field, FieldRequest req) {
        field.setName(req.getName());
        field.setSportType(req.getSportType());
        field.setDescription(req.getDescription());
        field.setSurfaceType(req.getSurfaceType());
        field.setCapacity(req.getCapacity());
        field.setPricePerHour(req.getPricePerHour());
        return field;
    }

    private void seedSportSpecificService(Facility facility, SportType sportType) {
        if (sportType == null) {
            return;
        }

        String serviceName;
        BigDecimal price;
        String unit;

        switch (sportType) {
            case BADMINTON -> {
                serviceName = "Cầu lông";
                price = new BigDecimal("25000");
                unit = "trái";
            }
            case FOOTBALL -> {
                serviceName = "Bóng";
                price = new BigDecimal("50000");
                unit = "trái";
            }
            default -> {
                return;
            }
        }

        if (extraServiceRepository.existsByFacility_IdAndNameIgnoreCase(facility.getId(), serviceName)) {
            return;
        }

        extraServiceRepository.save(ExtraService.builder()
                .facility(facility)
                .name(serviceName)
                .price(price)
                .unit(unit)
                .appliesToSportType(sportType)
                .isActive(true)
                .build());
    }
}
