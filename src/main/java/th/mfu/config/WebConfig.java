package th.mfu.config;

import java.time.Duration;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import th.mfu.app.HasRoomInterceptor;

/**
 * Language of the UI: Thai by default, English when chosen in Settings.
 * Any page accepts ?lang=th or ?lang=en; the choice is kept in the "lang" cookie.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    public static final Locale THAI = new Locale("th");

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setCookieName("lang");
        resolver.setCookieMaxAge((int) Duration.ofDays(365).getSeconds());
        resolver.setDefaultLocale(THAI);
        return resolver;
    }

    private final HasRoomInterceptor hasRoomInterceptor;

    public WebConfig(HasRoomInterceptor hasRoomInterceptor) {
        this.hasRoomInterceptor = hasRoomInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        interceptor.setIgnoreInvalidLocale(true);
        registry.addInterceptor(interceptor);
        // accounts without a room: only the home page, renting a room and their own profile
        registry.addInterceptor(hasRoomInterceptor).addPathPatterns("/app/**").excludePathPatterns("/app", "/app/rent", "/app/rent/**", "/app/profile", "/app/profile/**");
    }
}
