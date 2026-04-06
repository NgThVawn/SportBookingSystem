package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.repository.UserRepository;
import J2EE.SportBooingSystem.service.NotificationService;
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
    private final NotificationService notificationService;

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

    /** Số thông báo chưa đọc — dùng trong header để hiển thị badge */
    @ModelAttribute("unreadNotifCount")
    public long getUnreadCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return 0L;
        }
        User user = getLoggedInUser(authentication);
        if (user == null) return 0L;
        return notificationService.countUnread(user.getId());
    }

    private String resolveEmail(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2User principal = oauth2Token.getPrincipal();
            Object email = principal.getAttribute("email");
            if (email != null) return email.toString();
            Object fallback = principal.getAttribute("_principal_email");
            if (fallback != null) return fallback.toString();
            return null;
        }
        return authentication.getName();
    }
}