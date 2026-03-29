package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.RegisterRequest;
import J2EE.SportBooingSystem.entity.MembershipLevel;
import J2EE.SportBooingSystem.entity.Role;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.RoleName;
import J2EE.SportBooingSystem.exception.ResourceNotFoundException;
import J2EE.SportBooingSystem.repository.MembershipLevelRepository;
import J2EE.SportBooingSystem.repository.RoleRepository;
import J2EE.SportBooingSystem.repository.UserRepository;
import J2EE.SportBooingSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MembershipLevelRepository membershipLevelRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        MembershipLevel none = membershipLevelRepository.findByName("NONE")
            .orElseThrow(() -> new IllegalStateException("Membership levels not seeded"));

        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName(RoleName.USER)
            .orElseThrow(() -> new IllegalStateException("Roles not seeded")));
        if (req.isRegisterAsOwner()) {
            roles.add(roleRepository.findByName(RoleName.OWNER)
                .orElseThrow(() -> new IllegalStateException("Roles not seeded")));
        }

        User user = User.builder()
            .fullName(req.getFullName())
            .email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .phone(req.getPhone())
            .membershipLevel(none)
            .roles(roles)
            .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public void banUser(Long userId, String reason) {
        User user = findById(userId);
        user.setIsBanned(true);
        user.setBanReason(reason);
    }

    @Override
    public void unbanUser(Long userId) {
        User user = findById(userId);
        user.setIsBanned(false);
        user.setBanReason(null);
    }

    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    @Override
    public void updateProfile(String email, String fullName, String phone) {
        User user = findByEmail(email);
        user.setFullName(fullName);
        user.setPhone(phone);
    }
}
