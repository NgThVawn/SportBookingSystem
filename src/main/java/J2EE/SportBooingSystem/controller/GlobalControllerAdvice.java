package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserRepository userRepository;

    @ModelAttribute("user")
    public User getLoggedInUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        String email = resolveEmail(authentication);
        if (email == null) return null;

        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Giải quyết email từ authentication.
     *
     * - Form login (UsernamePasswordAuthenticationToken): getName() = email.
     * - OAuth2 login (OAuth2AuthenticationToken): getName() = provider ID (sub/id),
     *   KHÔNG phải email. Phải lấy từ attributes của principal.
     *   - Google OIDC: attribute "email" luôn có.
     *   - Facebook OAuth2: attribute "email" có nếu user cấp quyền,
     *     fallback "_principal_email" do CustomOAuth2UserService thêm vào.
     */
    private String resolveEmail(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User principal = oauth2Token.getPrincipal();

            Object email = principal.getAttribute("email");
            if (email != null) return email.toString();

            // Fallback cho Facebook không có email
            Object fallback = principal.getAttribute("_principal_email");
            if (fallback != null) return fallback.toString();

            return null;
        }
        // Form login: getName() trả về email trực tiếp
        return authentication.getName();
    }
}