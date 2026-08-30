package vn.taskconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Diem khoi dong duy nhat cua he thong.
 *
 * <p>TaskConnect la modular monolith: mot ung dung Spring Boot, mot tien trinh,
 * mot don vi trien khai. Cac module nghiep vu nam duoi package nay va goi nhau
 * bang interface Java noi bo, khong qua HTTP.
 *
 * <p>{@code @EnableScheduling} bat cac {@code @Scheduled} job trong toan ung dung
 * (vd AuthTokenCleanupScheduler) - khong co no thi annotation @Scheduled bi lang
 * lang bo qua, khong bao loi gi.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class TaskConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskConnectApplication.class, args);
    }
}
