package vn.taskconnect.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nguon thoi gian dung chung cua he thong, luon UTC (xem
 * spring.jpa.properties.hibernate.jdbc.time_zone).
 *
 * <p>Cac service nghiep vu inject {@link Clock} qua constructor thay vi goi
 * {@code Instant.now()} truc tiep, de test co the thay bang {@code Clock.fixed(...)}
 * ma khong can {@code Thread.sleep} (rule 40 cam Thread.sleep de cho bat dong bo/het han).
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
