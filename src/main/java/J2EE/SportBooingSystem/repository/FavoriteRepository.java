package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.Favorite;
import J2EE.SportBooingSystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUser(User user);

    Optional<Favorite> findByUserAndFacility(User user, Facility facility);

    boolean existsByUserAndFacility(User user, Facility facility);

    void deleteByUserAndFacility(User user, Facility facility);
}
