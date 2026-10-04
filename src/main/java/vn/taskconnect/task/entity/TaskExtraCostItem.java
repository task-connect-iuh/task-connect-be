package vn.taskconnect.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Mot khoan trong 1 batch chi phi phat sinh (task_extra_cost_items), vd "Cum voi rua gan tuong
 * moi" - xem V50. Bat buoc co name/amount, photoUrl tuy chon. Khong co method sua/xoa - batch cha
 * con PENDING thi Tasker chi co the Thu hoi toan bo batch roi dang lai batch moi, khong sua tung
 * khoan rieng le (don gian hoa UX, quyet dinh nguoi dung 2026-10-02).
 */
@Entity
@Table(name = "task_extra_cost_items")
public class TaskExtraCostItem {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "batch_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID batchId;

    @Column(name = "name", nullable = false, length = 255, updatable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "amount", nullable = false, updatable = false)
    private long amount;

    @Column(name = "photo_url", length = 500, updatable = false)
    private String photoUrl;

    @Column(name = "sort_order", nullable = false, updatable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TaskExtraCostItem() {
        // JPA
    }

    /** Tao 1 khoan moi trong batch - sortOrder giu dung thu tu Tasker da nhap trong form. */
    public static TaskExtraCostItem of(UUID id, UUID batchId, String name, long amount, String photoUrl,
            int sortOrder, Instant now) {
        TaskExtraCostItem item = new TaskExtraCostItem();
        item.id = id;
        item.batchId = batchId;
        item.name = name;
        item.amount = amount;
        item.photoUrl = photoUrl;
        item.sortOrder = sortOrder;
        item.createdAt = now;
        return item;
    }

    /** Id noi bo cua khoan nay. */
    public UUID getId() {
        return id;
    }

    /** Id batch cha. */
    public UUID getBatchId() {
        return batchId;
    }

    /** Ten khoan, bat buoc phai co gia tri. */
    public String getName() {
        return name;
    }

    /** So tien khoan nay, don vi dong. */
    public long getAmount() {
        return amount;
    }

    /** Anh minh chung, null neu khong dinh kem. */
    public String getPhotoUrl() {
        return photoUrl;
    }

    /** Vi tri hien thi trong batch, theo dung thu tu Tasker da nhap. */
    public int getSortOrder() {
        return sortOrder;
    }

    /** Thoi diem tao, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
