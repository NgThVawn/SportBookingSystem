package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.RegisterRequest;
import J2EE.SportBooingSystem.entity.MembershipLevel;
import J2EE.SportBooingSystem.entity.Role;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.RoleName;
import J2EE.SportBooingSystem.repository.MembershipLevelRepository;
import J2EE.SportBooingSystem.repository.RoleRepository;
import J2EE.SportBooingSystem.repository.UserRepository;
import J2EE.SportBooingSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
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
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
        MembershipLevel noneLevel = membershipLevelRepository.findByName("NONE")
                .orElseThrow();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .roles(new HashSet<>(Set.of(userRole)))
                .membershipLevel(noneLevel)
                .completedBookings(0)
                .isActive(true)
                .isBanned(false)
                .build();

        return userRepository.save(user);
    }
    @Override @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override @Transactional(readOnly = true)
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
    }
}
