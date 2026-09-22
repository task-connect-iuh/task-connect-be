package vn.taskconnect.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.taskconnect.task.api.ChangeType;

/**
 * Mot dong lich su de xuat gia (UC16 muc 3), CHI GHI THEM - khong UPDATE/DELETE dong da co, tru
 * dung 1 lan tren accept() (NULL -> gia tri, khong dao nguoc). Xem
 * V30__create_task_price_history_table.sql va TaskConnect_Chat_ImplementationSpec.md muc 3, 12.
 */
@Entity
@Table(name = "task_price_history")
public class TaskPriceHistory {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "application_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20, updatable = false)
    private ChangeType changeType;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "amount", nullable = false, updatable = false)
    private long amount;

    @Column(name = "note", columnDefinition = "TEXT", updatable = false)
    private String note;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "created_by_account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID createdByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "accepted_by_account_id", columnDefinition = "BINARY(16)")
    private UUID acceptedByAccountId;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected TaskPriceHistory() {
        // JPA
    }

    /** Tao 1 de xuat gia moi - accepted_by_account_id/accepted_at de trong, chi dien khi duoc Dong y (xem accept()). */
    public static TaskPriceHistory propose(UUID id, UUID applicationId, ChangeType changeType, long amount,
            String note, UUID createdByAccountId, Instant now) {
        TaskPriceHistory history = new TaskPriceHistory();
        history.id = id;
        history.applicationId = applicationId;
        history.changeType = changeType;
        history.amount = amount;
        history.note = note;
        history.createdByAccountId = createdByAccountId;
        history.createdAt = now;
        return history;
    }

    /**
     * Ghi nhan de xuat duoc Dong y - CHI duoc goi dung 1 lan (accepted_at con NULL), khong the
     * dao nguoc sau do (dac ta muc 12). Kiem tra dieu kien "chua tung Dong y" o day (entity),
     * dieu kien "ai duoc phep Dong y" (khong phai chinh nguoi tao) kiem tra o ChatService truoc
     * khi goi toi day.
     */
    public void accept(UUID accepterAccountId, Instant now) {
        if (this.acceptedAt != null) {
            throw new IllegalStateException("De xuat gia nay da duoc dong y truoc do.");
        }
        this.acceptedByAccountId = accepterAccountId;
        this.acceptedAt = now;
    }

    /** Id noi bo cua dong lich su gia nay. */
    public UUID getId() {
        return id;
    }

    /** Id don ung tuyen ma de xuat nay thuoc ve. */
    public UUID getApplicationId() {
        return applicationId;
    }

    /** INITIAL_AGREEMENT (truoc ASSIGNED) hay SCOPE_CHANGE (sau ASSIGNED). */
    public ChangeType getChangeType() {
        return changeType;
    }

    /** So tien de xuat, don vi dong. */
    public long getAmount() {
        return amount;
    }

    /** Ghi chu kem theo de xuat, co the null. */
    public String getNote() {
        return note;
    }

    /** Id tai khoan tao de xuat nay (Tasker hoac Poster). */
    public UUID getCreatedByAccountId() {
        return createdByAccountId;
    }

    /** Thoi diem tao de xuat, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Id tai khoan da Dong y, null neu chua duoc Dong y (bi tu choi/thu hoi van giu null vinh vien). */
    public UUID getAcceptedByAccountId() {
        return acceptedByAccountId;
    }

    /** Thoi diem duoc Dong y, null neu chua duoc Dong y. */
    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}
