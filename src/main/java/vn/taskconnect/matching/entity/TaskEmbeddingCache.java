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
 * Cache embedding vector cua mo ta mot Task, tinh mot lan va tai su dung cho moi lan goi y
 * Tasker cua task do (xem TaskerMatchingService/V35__create_matching_tables.sql). taskId la
 * khoa chinh - moi Task chi co dung mot cache, khac TaskerProfileEmbedding (theo cap
 * account+category) vi mot Task chi thuoc dung mot category/mo ta duy nhat, khong can phan
 * biet them chieu nao khac.
 */
@Entity
@Table(name = "matching_task_embedding_cache")
public class TaskEmbeddingCache {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "task_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID taskId;

    @Column(name = "vector", nullable = false, columnDefinition = "TEXT")
    private String vector;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TaskEmbeddingCache() {
        // JPA
    }

    /** Tao moi cache embedding cho mot Task - chi tao mot lan, khong co method update (Task khong sua mo ta o dot nay). */
    public static TaskEmbeddingCache create(UUID taskId, String vectorJson, String model, Instant now) {
        TaskEmbeddingCache cache = new TaskEmbeddingCache();
        cache.taskId = taskId;
        cache.vector = vectorJson;
        cache.model = model;
        cache.createdAt = now;
        return cache;
    }

    /** Id cong viec (cung la khoa chinh) embedding nay dai dien. */
    public UUID getTaskId() {
        return taskId;
    }

    /** Chuoi JSON cua mang float bieu dien vector, vd "[0.1,0.2,...]". */
    public String getVector() {
        return vector;
    }

    /** Ten/phien ban model da sinh ra vector nay. */
    public String getModel() {
        return model;
    }

    /** Thoi diem cache duoc tinh lan dau, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
