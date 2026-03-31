package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.FacilityRequest;
import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.FacilityImage;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.SportType;
import J2EE.SportBooingSystem.exception.ResourceNotFoundException;
import J2EE.SportBooingSystem.repository.FacilityRepository;
import J2EE.SportBooingSystem.service.FacilityService;
import J2EE.SportBooingSystem.service.FileStorageService;
import J2EE.SportBooingSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FacilityServiceImpl implements FacilityService {

    private final FacilityRepository facilityRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Override
    public Facility create(FacilityRequest req, String ownerEmail) {
        User owner = userService.findByEmail(ownerEmail);
        Facility facility = mapToEntity(new Facility(), req);
        facility.setOwner(owner);

        if (req.getImages() != null) {
            for (MultipartFile file : req.getImages()) {
                if (!file.isEmpty()) {
                    String url = fileStorageService.store(file);

                    FacilityImage img = FacilityImage.builder()
                            .facility(facility)
                            .imageUrl(url)
                            .isPrimary(facility.getImages().isEmpty()) 
                            .build();
                    facility.getImages().add(img);
                }
            }
        }

        return facilityRepository.save(facility);
    }

    @Override
    public Facility update(Long id, FacilityRequest req, String ownerEmail) {
        Facility facility = findById(id);
        if (!facility.getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Not authorized to edit this facility");
        }

        facility = mapToEntity(facility, req);

        if (req.getDeletedImageIds() != null && !req.getDeletedImageIds().isEmpty()) {
            facility.getImages().removeIf(img -> req.getDeletedImageIds().contains(img.getId()));

            if (!facility.getImages().isEmpty()) {
                boolean hasPrimary = facility.getImages().stream().anyMatch(FacilityImage::getIsPrimary);
                if (!hasPrimary) {
                    facility.getImages().get(0).setIsPrimary(true);
                }
            }
        }

        if (req.getImages() != null) {
            for (MultipartFile file : req.getImages()) {
                if (!file.isEmpty()) {
                    String url = fileStorageService.store(file);
                    FacilityImage img = FacilityImage.builder()
                            .facility(facility)
                            .imageUrl(url)
                            .isPrimary(facility.getImages().isEmpty()) 
                            .build();
                    facility.getImages().add(img);
                }
            }
        }

        return facilityRepository.save(facility);
    }
    @Override
    public void delete(Long id, String ownerEmail) {

        Facility facility = findById(id);
        
        if (!facility.getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Not authorized to delete this facility");
        }
        
        facilityRepository.delete(facility);
    }

    @Override
    @Transactional(readOnly = true)
    public Facility findById(Long id) {
        Facility facility = facilityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Facility not found: " + id));
            

        facility.getImages().size(); 
        
        return facility;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Facility> search(String city, SportType sport, String name, Pageable pageable) {
        return facilityRepository.searchFacilities(
            (city != null && !city.isBlank()) ? city : null,
            sport,
            (name != null && !name.isBlank()) ? name : null,
            pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Facility> findAll() {
        return facilityRepository.findByIsActiveTrueOrderByAvgRatingDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Facility> findByOwner(String ownerEmail) {
        return facilityRepository.findByOwner(userService.findByEmail(ownerEmail));
    }

    @Override
    public void toggleActive(Long id, String ownerEmail) {
        Facility facility = findById(id);
        if (!facility.getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Not authorized");
        }
        facility.setIsActive(!facility.getIsActive());
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return facilityRepository.countByIsActiveTrue();
    }

    private Facility mapToEntity(Facility facility, FacilityRequest req) {
        facility.setName(req.getName());
        facility.setDescription(req.getDescription());
        facility.setAddress(req.getAddress());
        facility.setCity(req.getCity());
        facility.setDistrict(req.getDistrict());
        facility.setLatitude(req.getLatitude());
        facility.setLongitude(req.getLongitude());
        facility.setPhone(req.getPhone());
        facility.setEmail(req.getEmail());
        if (req.getOpenTime() != null)
            facility.setOpenTime(LocalTime.parse(req.getOpenTime()));
        if (req.getCloseTime() != null)
            facility.setCloseTime(LocalTime.parse(req.getCloseTime()));
        return facility;
    }
}