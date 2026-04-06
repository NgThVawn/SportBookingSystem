package J2EE.SportBooingSystem.security.oauth2;

import java.util.Map;

/**
 * Facebook Graph API trả về:
 * id, name, email (nếu user cấp quyền), picture (object lồng nhau)
 *
 * Cấu hình user-info-uri đã thêm fields=id,name,email,picture.width(400)
 * nên picture trả về dạng: { "data": { "url": "...", "width": 400, "height": 400 } }
 */
public class FacebookOAuth2UserInfo extends OAuth2UserInfo {

    public FacebookOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        // Email có thể null nếu user không cấp quyền email cho app Facebook
        return (String) attributes.get("email");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getImageUrl() {
        // Cấu trúc: picture -> data -> url
        Map<String, Object> pictureObj = (Map<String, Object>) attributes.get("picture");
        if (pictureObj != null) {
            Map<String, Object> pictureData = (Map<String, Object>) pictureObj.get("data");
            if (pictureData != null) {
                return (String) pictureData.get("url");
            }
        }
        // Fallback: build URL từ userId
        return "https://graph.facebook.com/" + getId() + "/picture?type=large";
    }
}