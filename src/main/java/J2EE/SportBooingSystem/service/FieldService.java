package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.FieldRequest;
import J2EE.SportBooingSystem.entity.Field;

import java.util.List;

public interface FieldService {
    Field create(Long facilityId, FieldRequest request, String ownerEmail);
    Field update(Long fieldId, FieldRequest request, String ownerEmail);
    Field findById(Long id);
    List<Field> findByFacility(Long facilityId);
    void changeStatus(Long fieldId, String status, String ownerEmail);
}
