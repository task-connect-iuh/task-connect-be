package vn.taskconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Diem khoi dong duy nhat cua he thong.
 *
 * <p>TaskConnect la modular monolith: mot ung dung Spring Boot, mot tien trinh,
 * mot don vi trien khai. Cac module nghiep vu nam duoi package nay va goi nhau
 * bang interface Java noi bo, khong qua HTTP.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TaskConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskConnectApplication.class, args);
    }
}
