package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.RegisterRequest;
import J2EE.SportBooingSystem.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    User register(RegisterRequest request);
    User findByEmail(String email);
    User findById(Long id);
    User getCurrentUser(String email);
    List<User> findAll();
    void banUser(Long targetUserId, String reason, String actionUserEmail);
    void unbanUser(Long targetUserId, String actionUserEmail);  
    void changePassword(String email, String oldPassword, String newPassword);
    void updateProfile(String email, String fullName, String phone, MultipartFile avatar);
    void promoteToAdmin(Long targetUserId, String actionUserEmail);    
    void demoteFromAdmin(Long targetUserId, String currentSuperAdminEmail);
}
