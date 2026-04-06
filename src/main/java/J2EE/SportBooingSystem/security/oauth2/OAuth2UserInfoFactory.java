package J2EE.SportBooingSystem.security.oauth2;

import J2EE.SportBooingSystem.enums.AuthProvider;

import java.util.Map;

/**
 * Factory tạo đúng implementation OAuth2UserInfo dựa vào provider.
 */
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId,
                                                   Map<String, Object> attributes) {
        String provider = registrationId.toUpperCase();

        if (AuthProvider.GOOGLE.name().equals(provider)) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (AuthProvider.FACEBOOK.name().equals(provider)) {
            return new FacebookOAuth2UserInfo(attributes);
        } else {
            throw new IllegalArgumentException("Provider '" + registrationId + "' chưa được hỗ trợ.");
        }
    }
}