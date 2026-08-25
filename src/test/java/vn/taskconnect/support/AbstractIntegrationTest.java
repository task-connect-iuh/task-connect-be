package vn.taskconnect.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base cho integration test: DB va Redis that qua Testcontainers, khong dung H2
 * (rule 40 - FULLTEXT va kieu du lieu khac nhau, test xanh ma production do).
 *
 * <p>Container tinh (static), dung chung cho ca lop test con de khoi dong mot lan.
 * Khong container cho RabbitMQ: spring-boot-starter-amqp khong ket noi luc khoi dong
 * (CachingConnectionFactory la lazy), nen context khoi dong duoc ma khong can broker
 * that trong test.
 */
@Testcontainers
@SpringBootTest
@Import(SyncMailTestConfig.class)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"));

    @Container
    @ServiceConnection
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
}
