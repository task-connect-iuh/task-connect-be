package vn.taskconnect.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Mot anh minh hoa cua mot cong viec. Xem V18__create_task_tables.sql - toi da 5 anh/cong
 * viec, rang buoc nay kiem tra o TaskService (tang service), khong o DB. Khong dung quan he
 * JPA @OneToMany tu Task - dung field UUID taskId thuan tuy va repository rieng
 * (TaskImageRepository), theo dung phong cach cac entity 1-n khac trong du an (vd
 * TaskerCertification tham chieu accountId/categoryId, khong co quan he doi tuong).
 */
@Entity
@Table(name = "task_task_images")
public class TaskImage {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "task_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID taskId;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /** Vi tri hien thi trong danh sach anh cua cong viec, bat dau tu 0 theo thu tu nguoi dung da chon. */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected TaskImage() {
        // JPA
    }

    public TaskImage(UUID id, UUID taskId, String imageUrl, int displayOrder) {
        this.id = id;
        this.taskId = taskId;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    /** Id noi bo cua dong anh. */
    public UUID getId() {
        return id;
    }

    /** Id cong viec so huu anh nay. */
    public UUID getTaskId() {
        return taskId;
    }

    /** URL cong khai cua anh (S3, xem TaskImageUploadService). */
    public String getImageUrl() {
        return imageUrl;
    }

    /** Vi tri hien thi, dung de sap xep lai dung thu tu nguoi dung da chon luc dang. */
    public int getDisplayOrder() {
        return displayOrder;
    }
}
