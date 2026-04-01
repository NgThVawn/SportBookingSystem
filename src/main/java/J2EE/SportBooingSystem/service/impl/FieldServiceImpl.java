package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.FieldRequest;
import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.Field;
import J2EE.SportBooingSystem.enums.FieldStatus;
import J2EE.SportBooingSystem.exception.ResourceNotFoundException;
import J2EE.SportBooingSystem.repository.FieldRepository;
import J2EE.SportBooingSystem.service.FacilityService;
import J2EE.SportBooingSystem.service.FieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FieldServiceImpl implements FieldService {

    private final FieldRepository fieldRepository;
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
        return fieldRepository.save(field);
    }

    @Override
    public Field update(Long fieldId, FieldRequest req, String ownerEmail) {
        Field field = findById(fieldId);
        if (!field.getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Not authorized");
        }
        return fieldRepository.save(mapToEntity(field, req));
    }

    @Override
    @Transactional(readOnly = true)
    public Field findById(Long id) {
        return fieldRepository.findById(id)
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
}