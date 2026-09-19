package vn.taskconnect.matching.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.matching.entity.TaskerProfileEmbedding;

/**
 * Truy xuat du lieu bang matching_tasker_embeddings. Chi module Matching duoc inject truc
 * tiep repository nay.
 */
public interface TaskerProfileEmbeddingRepository extends JpaRepository<TaskerProfileEmbedding, UUID> {

    /** Cache embedding cua dung mot cap Tasker+category - UNIQUE trong V26 migration. */
    Optional<TaskerProfileEmbedding> findByAccountIdAndCategoryId(UUID accountId, UUID categoryId);
}
