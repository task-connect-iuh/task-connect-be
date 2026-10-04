package vn.taskconnect.task.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.taskconnect.task.api.dto.ExtraCostBatchSummary;
import vn.taskconnect.task.api.dto.PriceProposalCreated;
import vn.taskconnect.task.api.dto.TaskApplicationParties;
import vn.taskconnect.task.api.dto.TaskEditSummary;
import vn.taskconnect.task.api.dto.TaskSummary;

/**
 * Be mat cong khai duy nhat cua module Task. Module khac chi duoc goi qua day, cam import
 * entity trong {@code task.entity} hoac inject repository cua module Task.
 * entity trong {@code task.entity} hoac inject repository cua module Task. Mo rong tu ban
 * toi gian ban dau khi module Matching (goi y Tasker + moi lam viec) can doc/doi trang thai
 * Task ma khong duoc inject TaskRepository truc tiep.
 */
public interface TaskFacade {

    /** Doc thong tin toi thieu mot cong viec theo id, rong neu khong ton tai. */
    Optional<TaskSummary> findTask(UUID taskId);

    /**
     * Poster chap nhan mot Tasker cho cong viec nay (dung boi Matching khi Tasker accept
     * mot loi moi, tuong duong hanh vi TaskApplicationService.confirm() da co cho luong ung
     * tuyen) - chuyen Task tu OPEN sang ASSIGNED. Nem BusinessException(TASK_NOT_FOUND) neu
     * task khong ton tai hoac khong thuoc ve posterId, TASK_ALREADY_ASSIGNED neu task khong
     * con OPEN. Goi lai dung logic da co trong module Task (khong tao nhanh doi trang thai
     * song song), xem TaskFacadeImpl.
     */
    void assignTask(UUID taskId, UUID posterId);

    /**
     * Thong tin 2 ben (Poster/Tasker) cua 1 don ung tuyen, dung boi Chat de kiem tra quyen xem
     * kenh va dung cho Inbox. Rong neu applicationId khong ton tai.
     */
    Optional<TaskApplicationParties> getApplicationParties(UUID applicationId);

    /**
     * Toan bo applicationId ma accountId la Poster (chu task) hoac Tasker (nguoi gui don) -
     * dung boi Chat de liet ke Inbox cua 1 tai khoan.
     */
    List<UUID> listApplicationIdsForAccount(UUID accountId);

    /**
     * Tao 1 de xuat gia moi (UC16 muc 3) - CHI duoc goi tu Chat (khong co endpoint rieng o
     * Task). change_type tu dong theo task.status hien tai, 409 neu dang co 1 de xuat khac
     * chua xu ly (kiem tra qua ChatFacade).
     */
    PriceProposalCreated proposePrice(UUID applicationId, UUID proposerAccountId, long amount, String note);

    /**
     * Ghi nhan 1 de xuat gia duoc Dong y - tu nang INQUIRING len PENDING neu can, cap nhat
     * application.proposed_price. Tra ve amount da chot de Chat dung trong SYSTEM message.
     */
    long acceptPriceProposal(UUID applicationId, UUID priceHistoryId, UUID accepterAccountId);

    /** Doc amount cua 1 dong lich su gia theo id - dung khi Chat can hien lai de xuat cu. */
    Optional<Long> findPriceHistoryAmount(UUID priceHistoryId);

    /**
     * Xoa han het han loi moi (chi co tac dung neu applicationId dang INVITED va
     * actingAccountId chinh la Tasker cua don do) - goi VO DIEU KIEN tu Chat moi khi Tasker co
     * hanh dong trong kenh (gui TEXT, tao/chap nhan PRICE_PROPOSAL, dac ta muc 8, Round B5).
     * Khong lam gi trong moi truong hop khac, an toan de goi bat ky luc nao.
     */
    void clearInviteExpiryIfInvited(UUID applicationId, UUID actingAccountId);

    /**
     * Chi tiet 1 lan Poster sua cong viec (UC07) - dung boi Chat khi nguoi dung bam "Xem chi
     * tiet thay doi" tren SYSTEM message. Rong neu id khong ton tai. KHONG tu kiem tra quyen xem
     * o day - Chat da tu xac dinh nguoi goi la 1 trong 2 ben cua channel truoc khi goi (giong
     * convention cua findPriceHistoryAmount).
     */
    Optional<TaskEditSummary> findTaskEdit(UUID taskEditEventId);

    /**
     * Dong bo scheduledAt cua Task theo gio moi vua duoc dong y qua RESCHEDULE_PROPOSAL (UC16
     * muc 9, sau ASSIGNED) - dung boi Booking ngay sau BookingFacade.acceptReschedule() de
     * Task.scheduledAt luon la nguon duy nhat phan anh "gio dang hen" hien tai, khong lech voi
     * Booking.scheduledAt. Khong lien quan UC07 (Poster tu sua truoc ASSIGNED) - khong tao
     * TaskEditEvent, khong kiem tra quyen (Booking da tu dam bao goi dung ngu canh).
     */
    void syncScheduledAt(UUID taskId, Instant scheduledAt);

    /**
     * Chi tiet 1 batch "Chi phi phat sinh" theo id - dung boi Chat de hien tin nhan
     * EXTRA_COST_BATCH ngay trong khung chat (giong co che refPriceHistoryId/
     * findPriceHistoryAmount cho PRICE_PROPOSAL, bo sung 2026-10-03). Rong neu batchId khong ton
     * tai. Khong tu kiem tra quyen xem o day - Chat da tu xac dinh nguoi goi la 1 trong 2 ben cua
     * channel truoc khi goi toi day (giong convention cua findPriceHistoryAmount/findTaskEdit).
     */
    Optional<ExtraCostBatchSummary> findExtraCostBatch(UUID batchId);
}
