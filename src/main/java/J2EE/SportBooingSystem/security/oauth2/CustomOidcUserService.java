package J2EE.SportBooingSystem.security.oauth2;

import J2EE.SportBooingSystem.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service xử lý OIDC login – dùng cho Google (và bất kỳ OIDC provider nào).
 *
 * Vấn đề mặc định:
 *   OidcUserService tạo DefaultOidcUser với nameAttributeKey = "sub" (Google numeric ID).
 *   → authentication.getName() = "110023793312917997441"
 *   → @AuthenticationPrincipal UserDetails ud = null (DefaultOidcUser không impl UserDetails)
 *
 * Giải pháp:
 *   Wrap DefaultOidcUser trong OidcUserAdapter implements OidcUser + UserDetails.
 *   getName() = getUsername() = email từ DB → tất cả controller hoạt động đúng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Lấy DefaultOidcUser từ Spring Security (xử lý ID token + UserInfo endpoint)
        OidcUser oidcUser = super.loadUser(userRequest);

        // 2. Chuẩn hóa thông tin qua GoogleOAuth2UserInfo
        //    Google OIDC attributes chứa: sub, name, email, picture, email_verified
        GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(oidcUser.getAttributes());

        log.info("OIDC login: provider=google, email={}, name={}",
                userInfo.getEmail(), userInfo.getName());

        // 3. Tạo hoặc cập nhật user trong DB (dùng chung logic với CustomOAuth2UserService)
        User user = customOAuth2UserService.processOAuth2User("google", userInfo);

        // 4. Build authorities từ roles của user trong DB
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();

        // 5. Trả về OidcUserAdapter – implements OidcUser + UserDetails
        //    getName() = getUsername() = email → tất cả controller không cần sửa
        return new OidcUserAdapter(oidcUser, user.getEmail(), authorities);
    }
}
