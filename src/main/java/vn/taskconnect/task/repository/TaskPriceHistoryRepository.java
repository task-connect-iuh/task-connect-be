package vn.taskconnect.task.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.task.entity.TaskPriceHistory;

/**
 * Truy xuat du lieu bang task_price_history (append-only). Chi module Task duoc inject truc
 * tiep repository nay - module khac phai goi qua TaskFacade.
 */
public interface TaskPriceHistoryRepository extends JpaRepository<TaskPriceHistory, UUID> {

    /** Toan bo lich su gia cua 1 application, cu nhat truoc - dung cho man "Lich su gia". */
    List<TaskPriceHistory> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    /** Dong da duoc Dong y gan nhat (neu co) - "gia da chot" hien tai cua 1 application. */
    Optional<TaskPriceHistory> findTopByApplicationIdAndAcceptedAtIsNotNullOrderByAcceptedAtDesc(UUID applicationId);

    /**
     * Dong moi nhat bat ke da Dong y hay chua - CHI dung sau khi da xac nhan qua
     * ChatFacade.hasPendingProposal() rang dang thuc su co 1 de xuat PROPOSED, vi tu rieng bang
     * nay khong phan biet duoc "dang cho" voi "da Tu choi/Thu hoi" (ca 2 deu accepted_at NULL).
     */
    Optional<TaskPriceHistory> findTopByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
}
