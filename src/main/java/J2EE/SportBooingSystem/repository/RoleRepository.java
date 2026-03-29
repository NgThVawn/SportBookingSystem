package J2EE.SportBooingSystem.repository;

import J2EE.SportBooingSystem.entity.Role;
import J2EE.SportBooingSystem.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(RoleName name);
}
