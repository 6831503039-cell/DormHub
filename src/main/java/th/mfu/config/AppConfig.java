package th.mfu.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DormProperties.class)
public class AppConfig {

    // Cloud Run runs in UTC; the dorm lives in Thailand
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Bangkok"));
    }
}
