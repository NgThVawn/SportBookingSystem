package J2EE.SportBooingSystem.service;

public interface FavoriteService {

    boolean toggleFavorite(Long facilityId, String userEmail);
    
    boolean isFavorited(Long facilityId, String userEmail);
}