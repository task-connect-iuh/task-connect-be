package vn.taskconnect.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Cache embedding vector cua ho so mot Tasker cho MOT nhom dich vu (bio + facts ky nang),
 * dung cho buoc rerank ngu nghia cua module Matching (xem
 * TaskerMatchingService/V26__create_matching_tables.sql). vector luu duoi dang chuoi JSON
 * cua mang float trong cot TEXT (khong dung kieu vector rieng vi MariaDB hien tai khong ho
 * tro, va quy mo brute-force cosine trong Java la du - xem plan da duyet, phan "Vector DB
 * that"). Serialize/deserialize JSON thuc hien o tang service goi entity nay, khong o day.
 * UNIQUE (account_id, category_id): moi Tasker chi co dung mot embedding cache cho mot
 * category, tinh lai (ghi de) khi ho so thay doi.
 */
@Entity
@Table(name = "matching_tasker_embeddings")
public class TaskerProfileEmbedding {

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

    @Column(name = "vector", nullable = false, columnDefinition = "TEXT")
    private String vector;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskerProfileEmbedding() {
        // JPA
    }

    /** Tao moi cache embedding cho dung mot cap Tasker+category. */
    public static TaskerProfileEmbedding create(UUID id, UUID accountId, UUID categoryId, String vectorJson,
            String model, Instant now) {
        TaskerProfileEmbedding embedding = new TaskerProfileEmbedding();
        embedding.id = id;
        embedding.accountId = accountId;
        embedding.categoryId = categoryId;
        embedding.vector = vectorJson;
        embedding.model = model;
        embedding.updatedAt = now;
        return embedding;
    }

    /** Ghi de vector cache khi ho so Tasker thay doi (bio/ky nang) can tinh lai embedding. */
    public void updateVector(String vectorJson, String model, Instant now) {
        this.vector = vectorJson;
        this.model = model;
        this.updatedAt = now;
    }

    /** Id noi bo cua ban ghi cache nay. */
    public UUID getId() {
        return id;
    }

    /** Id tai khoan Tasker so huu embedding nay. */
    public UUID getAccountId() {
        return accountId;
    }

    /** Id nhom dich vu embedding nay dai dien. */
    public UUID getCategoryId() {
        return categoryId;
    }

    /** Chuoi JSON cua mang float bieu dien vector, vd "[0.1,0.2,...]". */
    public String getVector() {
        return vector;
    }

    /** Ten/phien ban model da sinh ra vector nay, dung de phat hien can tinh lai khi doi model. */
    public String getModel() {
        return model;
    }

    /** Thoi diem cache duoc tinh/cap nhat gan nhat. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
