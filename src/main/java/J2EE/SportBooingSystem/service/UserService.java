package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.RegisterRequest;
import J2EE.SportBooingSystem.entity.User;

import java.util.Optional;

public interface UserService {
    User register(RegisterRequest request);
    Optional<User> findByEmail(String email);
    User getCurrentUser();
}
