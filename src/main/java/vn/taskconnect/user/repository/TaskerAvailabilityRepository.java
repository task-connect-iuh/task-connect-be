package vn.taskconnect.user.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.user.entity.TaskerAvailability;

/**
 * Truy xuat du lieu bang user_tasker_availability. Chi module User duoc inject truc tiep
 * repository nay - module khac (vd Matching sau nay) phai goi qua UserFacade.
 */
public interface TaskerAvailabilityRepository extends JpaRepository<TaskerAvailability, UUID> {

    /** Toan bo khung gio ranh cua mot Tasker, sap theo thu roi gio bat dau - dung cho man hinh "lich ranh cua toi". */
    List<TaskerAvailability> findByAccountIdOrderByDayOfWeekAscStartTimeAsc(UUID accountId);
}
