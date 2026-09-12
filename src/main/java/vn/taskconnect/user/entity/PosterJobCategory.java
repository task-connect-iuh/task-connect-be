package vn.taskconnect.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Mot nhom dich vu ma Task Poster khai la "thuong thue" ("Ban thuong thue viec gi" trong
 * phan Gioi thieu ngan tren ho so). Xem V23__create_user_poster_job_categories.sql - UNIQUE
 * (account_id, category_id), chi mang tinh mo ta boi canh cho Tasker doc truoc khi ung
 * tuyen, KHONG dung cho Matching/AI phan loai.
 */
@Entity
@Table(name = "user_poster_job_categories")
public class PosterJobCategory {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID accountId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "category_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID categoryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PosterJobCategory() {
        // JPA
    }

    public PosterJobCategory(UUID id, UUID accountId, UUID categoryId, Instant now) {
        this.id = id;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.createdAt = now;
    }

    /** Id noi bo cua dong khai bao nay. */
    public UUID getId() {
        return id;
    }

    /** Id tai khoan Poster so huu dong khai bao nay. */
    public UUID getAccountId() {
        return accountId;
    }

    /** Id nhom dich vu duoc khai la thuong thue. */
    public UUID getCategoryId() {
        return categoryId;
    }

    /** Thoi diem khai bao dong nay. */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
