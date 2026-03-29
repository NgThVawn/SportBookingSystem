package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.RegisterRequest;
import J2EE.SportBooingSystem.entity.User;

import java.util.List;

public interface UserService {
    User register(RegisterRequest request);
    User findByEmail(String email);
    User findById(Long id);
    List<User> findAll();
    void banUser(Long userId, String reason);
    void unbanUser(Long userId);
    void changePassword(String email, String oldPassword, String newPassword);
    void updateProfile(String email, String fullName, String phone);
}
