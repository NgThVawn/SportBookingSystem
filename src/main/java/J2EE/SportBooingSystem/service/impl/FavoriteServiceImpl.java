package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.Favorite;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.repository.FacilityRepository;
import J2EE.SportBooingSystem.repository.FavoriteRepository;
import J2EE.SportBooingSystem.repository.UserRepository;
import J2EE.SportBooingSystem.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepo;
    private final UserRepository userRepo;
    private final FacilityRepository facilityRepo;

    @Override
    @Transactional
    public boolean toggleFavorite(Long facilityId, String userEmail) {
        User user = userRepo.findByEmail(userEmail).orElseThrow();
        Facility facility = facilityRepo.findById(facilityId).orElseThrow();

        Optional<Favorite> existing = favoriteRepo.findByUserAndFacility(user, facility);
        
        if (existing.isPresent()) {
            favoriteRepo.delete(existing.get()); // Đã thả tim -> Xóa (Bỏ yêu thích)
            return false;
        } else {
            Favorite fav = Favorite.builder().user(user).facility(facility).build();
            favoriteRepo.save(fav); // Chưa thả tim -> Thêm mới
            return true;
        }
    }

    @Override
    public boolean isFavorited(Long facilityId, String userEmail) {
        if (userEmail == null || userEmail.equals("anonymousUser")) return false;
        
        User user = userRepo.findByEmail(userEmail).orElse(null);
        Facility facility = facilityRepo.findById(facilityId).orElse(null);
        
        if (user == null || facility == null) return false;
        return favoriteRepo.existsByUserAndFacility(user, facility);
    }
}