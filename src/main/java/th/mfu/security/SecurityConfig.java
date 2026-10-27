package th.mfu.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                .logoutSuccessUrl("/login?logout"));
        return http.build();
    }
}
