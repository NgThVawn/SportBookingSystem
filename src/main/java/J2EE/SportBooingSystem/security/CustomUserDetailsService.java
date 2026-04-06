package J2EE.SportBooingSystem.security;

import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Sai tài khoản hoặc mật khẩu"));

        if (user.getIsBanned() != null && user.getIsBanned()) {
            String reason = user.getBanReason() != null ? user.getBanReason() : "Vi phạm quy định của hệ thống";
            throw new LockedException("Tài khoản của bạn đã bị khóa! Lý do: " + reason);
        }

        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new DisabledException("Tài khoản của bạn chưa được kích hoạt hoặc đã bị vô hiệu hóa.");
        }

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
            .collect(Collectors.toList());

        String password = user.getPassword() != null ? user.getPassword() : "{noop}oauth2_user_no_password";

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(password)
                .authorities(authorities)
                .build();
    }
}