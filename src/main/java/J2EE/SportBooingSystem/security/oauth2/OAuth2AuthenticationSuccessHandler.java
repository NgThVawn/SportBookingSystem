package J2EE.SportBooingSystem.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý sau khi OAuth2 đăng nhập thành công.
 * Hiện tại redirect về trang chủ "/".
 * Mở rộng sau: có thể kiểm tra xem user có phone chưa → redirect điền thông tin bổ sung.
 */
@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        log.info("OAuth2 đăng nhập thành công: {}", authentication.getName());

        // Xóa thuộc tính session tạm thời nếu có
        clearAuthenticationAttributes(request);

        // Redirect về trang chủ
        getRedirectStrategy().sendRedirect(request, response, "/");
    }
}