package vn.taskconnect.support;

import java.util.concurrent.Executor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;

/**
 * Thay bean "notificationExecutor" (ThreadPoolTaskExecutor that, xem
 * NotificationAsyncConfig) bang SyncTaskExecutor trong test: method
 * {@code @Async("notificationExecutor")} chay dong bo tren cung luong goi, ket qua co
 * ngay khi method service tra ve - khong can Awaitility hay Thread.sleep de cho luong
 * bat dong bo (rule 40 cam Thread.sleep).
 *
 * <p>Ghi de theo ten bean nen can {@code spring.main.allow-bean-definition-overriding}
 * bat trong src/test/resources/application.yml.
 */
@TestConfiguration
public class SyncMailTestConfig {

    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        return new SyncTaskExecutor();
    }
}
