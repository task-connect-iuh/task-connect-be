package vn.taskconnect.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Mot khung gio ranh cua Tasker trong tuan (Buoc 7). Xem V2__create_user_tables.sql -
 * khong co UNIQUE tren (account_id, day_of_week), moi Tasker khai duoc nhieu khung gio cho
 * cung mot ngay (vd sang va chieu rieng biet). Khong co trang thai xac minh - day chi la
 * thong tin tu khai, khong qua duyet, doc lap voi cac buoc con lai cua module User.
 */
@Entity
@Table(name = "user_tasker_availability")
public class TaskerAvailability {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID accountId;

    // 1 = Thu 2 ... 7 = Chu nhat, dung khop CHECK constraint cua V2 migration.
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected TaskerAvailability() {
        // JPA
    }

    /** Khai bao mot khung gio ranh moi. */
    public TaskerAvailability(UUID id, UUID accountId, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.accountId = accountId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** Sua lai khung gio da khai bao - PATCH mot phan da duoc gop san o TaskerAvailabilityService. */
    public void update(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** Id noi bo cua khung gio nay. */
    public UUID getId() {
        return id;
    }

    /** Id tai khoan Tasker so huu khung gio nay. */
    public UUID getAccountId() {
        return accountId;
    }

    /** Thu trong tuan, 1 = Thu 2 ... 7 = Chu nhat. */
    public int getDayOfWeek() {
        return dayOfWeek;
    }

    /** Gio bat dau ranh. */
    public LocalTime getStartTime() {
        return startTime;
    }

    /** Gio ket thuc ranh. */
    public LocalTime getEndTime() {
        return endTime;
    }
}
