package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);



    @Query("SELECT COUNT(u) FROM User u WHERE u.isBanned = false AND u.isActive = true")
    long countActiveUsers();
}
