package J2EE.SportBooingSystem.controller.api;

import J2EE.SportBooingSystem.dto.response.ApiResponse;
import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.Favorite;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.repository.FavoriteRepository;
import J2EE.SportBooingSystem.service.FacilityService;
import J2EE.SportBooingSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteApiController {

    private final FavoriteRepository favoriteRepository;
    private final FacilityService facilityService;
    private final UserService userService;

    @PostMapping("/toggle/{facilityId}")
    @Transactional // CHỈ CẦN THÊM ĐÚNG DÒNG NÀY LÀ ĐỦ
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggle(
            @PathVariable Long facilityId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername());
        Facility facility = facilityService.findById(facilityId);

        boolean isFav = favoriteRepository.existsByUserAndFacility(user, facility);
        if (isFav) {
            favoriteRepository.deleteByUserAndFacility(user, facility);
        } else {
            favoriteRepository.save(Favorite.builder()
                    .user(user).facility(facility).build());
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of("isFavorite", !isFav)));
    }
}