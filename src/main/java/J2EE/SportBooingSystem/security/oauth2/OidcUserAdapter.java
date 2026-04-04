package J2EE.SportBooingSystem.security.oauth2;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;

/**
 * Adapter cho OIDC login (Google).
 *
 * Wraps DefaultOidcUser + implements UserDetails để:
 * - authentication.getName()                → trả email (không phải Google sub)
 * - @AuthenticationPrincipal UserDetails ud → ud không null, ud.getUsername() = email
 * - @AuthenticationPrincipal OidcUser u     → cũng hoạt động
 *
 * Tất cả phương thức OidcUser đều delegate về DefaultOidcUser gốc.
 */
public class OidcUserAdapter implements OidcUser, UserDetails {

    private final OidcUser delegate;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public OidcUserAdapter(OidcUser delegate,
                           String email,
                           Collection<? extends GrantedAuthority> authorities) {
        this.delegate    = delegate;
        this.email       = email;
        this.authorities = authorities;
    }

    // ── OidcUser (delegate) ─────────────────────────────────────────────────
    @Override public Map<String, Object> getClaims() { return delegate.getClaims(); }
    @Override public OidcUserInfo getUserInfo()      { return delegate.getUserInfo(); }
    @Override public OidcIdToken getIdToken()        { return delegate.getIdToken(); }

    // ── OAuth2User ──────────────────────────────────────────────────────────
    @Override public Map<String, Object> getAttributes() { return delegate.getAttributes(); }
    @Override public String getName() { return email; }

    // ── UserDetails ─────────────────────────────────────────────────────────
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return null; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
