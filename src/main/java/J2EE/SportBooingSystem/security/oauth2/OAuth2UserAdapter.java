package J2EE.SportBooingSystem.security.oauth2;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Adapter cho OAuth2 login (Facebook).
 *
 * Implements cả OAuth2User lẫn UserDetails để:
 * - authentication.getName()                    → trả email (không phải Facebook user ID)
 * - @AuthenticationPrincipal UserDetails ud     → ud không null, ud.getUsername() = email
 * - @AuthenticationPrincipal OAuth2User oauth2  → cũng hoạt động
 */
public class OAuth2UserAdapter implements OAuth2User, UserDetails {

    private final String email;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    public OAuth2UserAdapter(String email,
                             Map<String, Object> attributes,
                             Collection<? extends GrantedAuthority> authorities) {
        this.email = email;
        this.attributes = attributes;
        this.authorities = authorities;
    }

    // ── OAuth2User ──────────────────────────────────────────────────────────
    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getName() { return email; }

    // ── UserDetails ─────────────────────────────────────────────────────────
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return null; }  // OAuth2 user không có password
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
