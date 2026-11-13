package th.mfu.security;

import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .antMatchers("/login", "/register", "/css/**", "/img/**", "/js/**", "/error").permitAll()
                // public 3D room map and booking requests (no login needed)
                .antMatchers("/explore", "/explore/**").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                // REST API (th.mfu.api): admin only
                .antMatchers("/api/**").hasRole("ADMIN")
                .antMatchers("/app/**").hasRole("TENANT")
                .anyRequest().authenticated())
            // one login page for everyone; "/" then sends each role to its own area
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/", true)
                .permitAll())
            // the REST API can also be called from Postman with Basic Auth (admin email + password);
            // API calls send JSON, not a web form, so they skip the CSRF token
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.ignoringAntMatchers("/api/**"))
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
