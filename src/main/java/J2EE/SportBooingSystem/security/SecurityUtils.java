package J2EE.SportBooingSystem.security;

import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() ||
            auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        String email = resolveEmail(auth);
        if (email == null) return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return resolveEmail(auth);
    }

    /**
     * Lấy email từ authentication, xử lý đúng cả form login và OAuth2 login.
     * - Form login: auth.getName() = email.
     * - OAuth2 (Google OIDC): auth.getName() = sub ID → phải lấy từ attribute "email".
     * - OAuth2 (Facebook): auth.getName() = user ID → lấy từ attribute "email" hoặc "_principal_email".
     */
    private String resolveEmail(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User principal = oauth2Token.getPrincipal();
            Object email = principal.getAttribute("email");
            if (email != null) return email.toString();
            Object fallback = principal.getAttribute("_principal_email");
            if (fallback != null) return fallback.toString();
            return null;
        }
        return auth.getName();
    }

    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() &&
               !auth.getPrincipal().equals("anonymousUser");
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
