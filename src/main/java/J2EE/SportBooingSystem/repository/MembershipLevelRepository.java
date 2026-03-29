package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.MembershipLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipLevelRepository extends JpaRepository<MembershipLevel, Integer> {
    Optional<MembershipLevel> findByName(String name);
}
