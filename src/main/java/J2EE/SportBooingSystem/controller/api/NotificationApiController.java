package J2EE.SportBooingSystem.controller.api;

import J2EE.SportBooingSystem.dto.response.NotificationResponse;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.repository.UserRepository;
import J2EE.SportBooingSystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notifService;
    private final UserRepository userRepo;

    /** Lấy tất cả thông báo của user hiện tại */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll(Authentication auth) {
        User user = resolveUser(auth);
        return ResponseEntity.ok(notifService.getAll(user.getId()));
    }

    /** Đếm thông báo chưa đọc */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication auth) {
        User user = resolveUser(auth);
        return ResponseEntity.ok(Map.of("count", notifService.countUnread(user.getId())));
    }

    /** Đánh dấu 1 thông báo đã đọc */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Authentication auth) {
        User user = resolveUser(auth);
        notifService.markRead(id, user.getId());
        return ResponseEntity.ok().build();
    }

    /** Đánh dấu tất cả đã đọc */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        User user = resolveUser(auth);
        notifService.markAllRead(user.getId());
        return ResponseEntity.ok().build();
    }

    // ── Helper: resolve User từ cả form login và OAuth2 ──────────
    private User resolveUser(Authentication auth) {
        String email;
        if (auth instanceof OAuth2AuthenticationToken oauth2) {
            OAuth2User principal = oauth2.getPrincipal();
            Object emailAttr = principal.getAttribute("email");
            if (emailAttr != null) {
                email = emailAttr.toString();
            } else {
                Object fallback = principal.getAttribute("_principal_email");
                email = fallback != null ? fallback.toString() : null;
            }
        } else {
            email = auth.getName();
        }
        if (email == null) throw new IllegalStateException("Không xác định được email người dùng");
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user: " + email));
    }
}
