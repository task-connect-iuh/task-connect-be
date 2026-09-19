package vn.taskconnect.matching.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.matching.entity.TaskEmbeddingCache;

/**
 * Truy xuat du lieu bang matching_task_embedding_cache. Chi module Matching duoc inject
 * truc tiep repository nay. taskId la khoa chinh nen JpaRepository.findById(taskId) la du,
 * khong can them finder method rieng.
 */
public interface TaskEmbeddingCacheRepository extends JpaRepository<TaskEmbeddingCache, UUID> {
}
