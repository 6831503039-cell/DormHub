package th.mfu.security;

import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // REST API (th.mfu.api): admin only. It has its own chain so the web pages below keep working exactly as before.
    // Postman can log in with Basic Auth (admin email + password); API calls send JSON, not a web form,
    // so they do not need the CSRF token.
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.antMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .antMatchers("/login", "/register", "/css/**", "/img/**", "/js/**", "/error").permitAll()
                // public 3D room map and booking requests (no login needed)
                .antMatchers("/explore", "/explore/**").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/app/**").hasRole("TENANT")
                .anyRequest().authenticated())
            // one login page for everyone; "/" then sends each role to its own area
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout"))
            // a form sent after the session ended (server restart, another server instance, idle timeout)
            // has a stale CSRF token: send the user back to log in instead of showing a bare 403 page
            .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, denied) -> {
                if (!(denied instanceof CsrfException)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                } else if (String.valueOf(request.getHeader("Accept")).contains("application/json")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    response.sendRedirect(request.getContextPath() + "/login?expired");
                }
            }));
        return http.build();
    }
}
