package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.FacilityRequest;
import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.enums.FacilityStatus;
import J2EE.SportBooingSystem.enums.SportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FacilityService {
    Facility create(FacilityRequest request, String ownerEmail);
    Facility update(Long id, FacilityRequest request, String ownerEmail);
    Facility findById(Long id);

    // Đã cập nhật lại tham số cho khớp với Impl
    Page<Facility> search(String city, SportType sport, String name, Boolean favoritesOnly, Long userId, Pageable pageable);

    List<Facility> findAll();
    List<Facility> findByOwner(String ownerEmail);
    void changeStatus(Long id, FacilityStatus status, String ownerEmail);
    long count();
    void delete(Long id, String ownerEmail);
    Page<Facility> findAllForAdmin(Pageable pageable);
    void changeStatusByAdmin(Long id, J2EE.SportBooingSystem.enums.FacilityStatus status);
}