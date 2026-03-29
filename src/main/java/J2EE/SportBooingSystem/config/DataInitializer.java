package J2EE.SportBooingSystem.config;

import J2EE.SportBooingSystem.entity.MembershipLevel;
import J2EE.SportBooingSystem.entity.Role;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.RoleName;
import J2EE.SportBooingSystem.repository.MembershipLevelRepository;
import J2EE.SportBooingSystem.repository.RoleRepository;
import J2EE.SportBooingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MembershipLevelRepository membershipLevelRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdminUser();
    }

    private void seedAdminUser() {
        if (userRepository.existsByEmail("admin@sport.vn")) return;

        MembershipLevel none = membershipLevelRepository.findByName("NONE").orElse(null);
        if (none == null) {
            log.warn("Membership levels not found — skipping admin seed. " +
                     "Ensure data.sql ran successfully.");
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElse(null);
        Role userRole  = roleRepository.findByName(RoleName.USER).orElse(null);
        if (adminRole == null || userRole == null) {
            log.warn("Roles not found — skipping admin seed.");
            return;
        }

        User admin = User.builder()
            .fullName("System Administrator")
            .email("admin@sport.vn")
            .password(passwordEncoder.encode("Admin@123"))
            .phone("0900000000")
            .membershipLevel(none)
            .roles(Set.of(adminRole, userRole))
            .emailVerified(true)
            .build();

        userRepository.save(admin);
        log.info("=================================================");
        log.info("Default admin created: admin@sport.vn / Admin@123");
        log.info("CHANGE THIS PASSWORD IMMEDIATELY IN PRODUCTION!");
        log.info("=================================================");
    }
}
