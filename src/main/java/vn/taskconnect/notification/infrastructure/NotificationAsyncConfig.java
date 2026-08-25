package vn.taskconnect.notification.infrastructure;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor rieng cho luong gui thong bao, co gioi han. Luon goi bang
 * {@code @Async("notificationExecutor")}, khong bao gio dung {@code @Async} tran.
 *
 * <p>Luu y cho module sau: khai bao mot bean kieu {@link java.util.concurrent.Executor}
 * o day se tat {@code applicationTaskExecutor} mac dinh cua Spring Boot
 * ({@code TaskExecutionAutoConfiguration} co {@code @ConditionalOnMissingBean(Executor.class)}).
 * Module nao sau nay dung {@code @Async} hay MVC async phai tu khai bao executor rieng,
 * khong duoc ngam dinh dung lai cai nay.
 *
 * <p>Chinh sach tu choi khi hang doi day la {@link ThreadPoolExecutor.AbortPolicy}, KHONG
 * phai {@code CallerRunsPolicy}: CallerRuns se day loi goi SMTP nguoc len chinh luong dang
 * goi (co the la luong HTTP) va pha ngoung do tre API duoi 1 giay (rule 40).
 */
@Configuration
@EnableAsync
class NotificationAsyncConfig implements AsyncConfigurer {

    @Bean("notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ntf-mail-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
