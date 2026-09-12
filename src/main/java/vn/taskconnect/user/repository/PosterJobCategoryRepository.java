package vn.taskconnect.user.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.user.entity.PosterJobCategory;

/**
 * Truy xuat du lieu bang user_poster_job_categories. Chi module User duoc inject truc tiep
 * repository nay - module khac phai goi qua UserFacade.
 */
public interface PosterJobCategoryRepository extends JpaRepository<PosterJobCategory, UUID> {

    /** Danh sach nhom dich vu Poster da khai, dung cho ProfileResponse.jobCategoryIds. */
    List<PosterJobCategory> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    /** Xoa toan bo khai bao cu truoc khi ghi lai danh sach moi (PATCH thay the toan bo, khong cong don). */
    @Transactional
    void deleteByAccountId(UUID accountId);
}
