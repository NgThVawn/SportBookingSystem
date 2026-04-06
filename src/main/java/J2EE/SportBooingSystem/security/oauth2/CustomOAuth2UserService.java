package J2EE.SportBooingSystem.security.oauth2;

import J2EE.SportBooingSystem.entity.MembershipLevel;
import J2EE.SportBooingSystem.entity.Role;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.AuthProvider;
import J2EE.SportBooingSystem.enums.RoleName;
import J2EE.SportBooingSystem.repository.MembershipLevelRepository;
import J2EE.SportBooingSystem.repository.RoleRepository;
import J2EE.SportBooingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MembershipLevelRepository membershipLevelRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Gọi provider API để lấy attributes
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. Xác định provider (google / facebook)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. Chuẩn hóa thông tin qua factory
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oAuth2User.getAttributes());

        log.info("OAuth2 login: provider={}, email={}, name={}",
                registrationId, userInfo.getEmail(), userInfo.getName());

        // 5. Xử lý user trong DB
        User user = processOAuth2User(registrationId, userInfo);

        // 6. Trả về OAuth2UserAdapter – implements cả OAuth2User lẫn UserDetails.
        //    getName() = getUsername() = email từ DB (không phải Facebook/Google ID).
        //    → authentication.getName()                 = email ✓
        //    → @AuthenticationPrincipal UserDetails ud  = adapter (không null) ✓
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();

        Map<String, Object> mutableAttributes = new HashMap<>(oAuth2User.getAttributes());
        return new OAuth2UserAdapter(user.getEmail(), mutableAttributes, authorities);
    }

    public User processOAuth2User(String registrationId, OAuth2UserInfo userInfo) {
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // Thử tìm bằng providerId trước (user đã từng đăng nhập OAuth2)
        Optional<User> existingByProvider = userRepository
                .findByProviderAndProviderId(provider, userInfo.getId());

        if (existingByProvider.isPresent()) {
            return updateExistingUser(existingByProvider.get(), userInfo);
        }

        // Thử tìm bằng email (user đã đăng ký LOCAL với cùng email)
        if (userInfo.getEmail() != null) {
            Optional<User> existingByEmail = userRepository.findByEmail(userInfo.getEmail());
            if (existingByEmail.isPresent()) {
                User user = existingByEmail.get();
                // Liên kết tài khoản OAuth2 vào tài khoản LOCAL hiện có
                user.setProvider(provider);
                user.setProviderId(userInfo.getId());
                // Cập nhật avatar nếu user chưa có
                if (user.getAvatarUrl() == null && userInfo.getImageUrl() != null) {
                    user.setAvatarUrl(userInfo.getImageUrl());
                }
                user.setEmailVerified(true);
                return userRepository.save(user);
            }
        }

        // User hoàn toàn mới – tạo tài khoản
        return registerNewOAuth2User(provider, userInfo);
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        // Kiểm tra trạng thái tài khoản
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new LockedException("Tài khoản của bạn đã bị khóa! Lý do: "
                    + (user.getBanReason() != null ? user.getBanReason() : "Vi phạm quy định"));
        }
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new DisabledException("Tài khoản của bạn đã bị vô hiệu hóa.");
        }

        // Cập nhật avatar nếu provider có ảnh mới (ảnh từ provider luôn mới hơn ảnh mặc định)
        if (userInfo.getImageUrl() != null) {
            user.setAvatarUrl(userInfo.getImageUrl());
        }
        return userRepository.save(user);
    }

    private User registerNewOAuth2User(AuthProvider provider, OAuth2UserInfo userInfo) {
        // Lấy membership mặc định "NONE"
        MembershipLevel defaultMembership = membershipLevelRepository
                .findByName("NONE")
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy membership level 'NONE'. Hãy kiểm tra data.sql"));

        // Gán role USER
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role USER"));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User newUser = User.builder()
                .fullName(userInfo.getName() != null ? userInfo.getName() : "Người dùng")
                // Email có thể null (Facebook không cấp quyền) → dùng placeholder
                .email(userInfo.getEmail() != null
                        ? userInfo.getEmail()
                        : provider.name().toLowerCase() + "_" + userInfo.getId() + "@noemail.placeholder")
                .password(null)                     // OAuth2 user không có password
                .phone(null)                        // Cần điền bổ sung sau
                .avatarUrl(userInfo.getImageUrl())
                .membershipLevel(defaultMembership)
                .provider(provider)
                .providerId(userInfo.getId())
                .emailVerified(true)                // Email từ provider đã được xác thực
                .isActive(true)
                .isBanned(false)
                .completedBookings(0)
                .roles(roles)
                .build();

        return userRepository.save(newUser);
    }
}