package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.FacilityRequest;
import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.enums.SportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FacilityService {
    Facility create(FacilityRequest request, String ownerEmail);
    Facility update(Long id, FacilityRequest request, String ownerEmail);
    Facility findById(Long id);
    Page<Facility> search(String city, SportType sport, String name, Pageable pageable);
    List<Facility> findAll();
    List<Facility> findByOwner(String ownerEmail);
    void toggleActive(Long id, String ownerEmail);
    long count();
}