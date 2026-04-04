package J2EE.SportBooingSystem.config;

import J2EE.SportBooingSystem.security.CustomUserDetailsService;
import J2EE.SportBooingSystem.security.RoleBasedAuthenticationSuccessHandler;
import J2EE.SportBooingSystem.security.oauth2.CustomOAuth2UserService;
import J2EE.SportBooingSystem.security.oauth2.CustomOidcUserService;
import J2EE.SportBooingSystem.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final RoleBasedAuthenticationSuccessHandler roleBasedAuthenticationSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/", "/facilities/**", "/css/**", "/js/**",
                        "/images/**", "/error/**", "/uploads/**",
                        "/ws/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                // VNPay callbacks không cần auth
                .requestMatchers("/payment/vnpay-return", "/payment/vnpay-ipn",
                        "/payment/momo-return",  "/payment/momo-ipn").permitAll()
                // Owner area
                .requestMatchers("/owner/**").hasAnyRole("OWNER", "ADMIN", "SUPER_ADMIN")
                // Admin area
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // REST APIs
                .requestMatchers("/api/v1/facilities/**",
                                 "/api/v1/fields/**").permitAll()
                .requestMatchers("/api/**").authenticated()


                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(roleBasedAuthenticationSuccessHandler)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                    .loginPage("/auth/login")
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)   // Facebook (OAuth2)
                            .oidcUserService(customOidcUserService) // Google (OIDC)
                    )
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureUrl("/auth/login?error=oauth2")
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/api/**", "/payment/vnpay-ipn", "/payment/momo-ipn", "/ws/**")
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
            );

        return http.build();
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
