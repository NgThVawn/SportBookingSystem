package J2EE.SportBooingSystem.security.oauth2;

import java.util.Map;

/**
 * Abstraction lấy thông tin user từ bất kỳ OAuth2 provider nào.
 * Mỗi provider trả attributes theo cấu trúc riêng, interface này chuẩn hóa chúng.
 */
public abstract class OAuth2UserInfo {

    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** ID duy nhất do provider cấp (Google: "sub", Facebook: "id") */
    public abstract String getId();

    /** Họ tên đầy đủ */
    public abstract String getName();

    /** Địa chỉ email (có thể null với Facebook nếu user không cấp quyền) */
    public abstract String getEmail();

    /** URL avatar */
    public abstract String getImageUrl();
}
