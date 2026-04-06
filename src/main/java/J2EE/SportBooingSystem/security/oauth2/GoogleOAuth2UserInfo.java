package J2EE.SportBooingSystem.security.oauth2;

import java.util.Map;

/**
 * Google trả về các attributes chuẩn OIDC:
 * sub, name, email, email_verified, picture
 */
public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        // Google trả URL ảnh nhỏ mặc định, thay =s96-c bằng =s400-c để lấy ảnh lớn hơn
        String picture = (String) attributes.get("picture");
        if (picture != null) {
            return picture.replace("=s96-c", "=s400-c");
        }
        return null;
    }
}