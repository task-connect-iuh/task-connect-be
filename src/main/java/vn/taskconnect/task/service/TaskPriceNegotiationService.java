package vn.taskconnect.task.service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.task.api.ChangeType;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.api.dto.PriceProposalCreated;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.entity.TaskApplication;
import vn.taskconnect.task.entity.TaskPriceHistory;
import vn.taskconnect.task.repository.TaskApplicationRepository;
import vn.taskconnect.task.repository.TaskPriceHistoryRepository;
import vn.taskconnect.task.repository.TaskRepository;

/**
 * Nghiep vu thuong luong gia (UC16 muc 3) - phan sau ChatFacade.proposePrice()/
 * acceptPriceProposal(), chi duoc goi tu module Chat (khong co endpoint rieng o module Task -
 * dac ta muc 3: "chi duoc gui tu ben trong khung chat"). TaskFacadeImpl uy quyen toan bo cho
 * class nay, giong mau ChatService/ChatFacadeImpl da dung o Round B2.
 *
 * <p>KHONG phu thuoc ChatFacade o day du proposePrice() ve mat nghiep vu can kiem tra "dang
 * co de xuat PROPOSED khac chua" (du lieu do chi Chat co, qua cot proposal_status). Kiem tra
 * do duoc ChatService tu thuc hien TRUOC khi goi proposePrice() (xem ChatService.
 * createPriceProposal()), vi neu class nay inject ChatFacade se tao vong tron bean Spring:
 * ChatService -> TaskFacadeImpl -> TaskPriceNegotiationService -> ChatFacadeImpl -> ChatService
 * (da gap loi that khi chay thu, xem docs/PROGRESS-CHAT-MODULE.md Round B3).
 */
@Service
public class TaskPriceNegotiationService {

    private final TaskApplicationRepository applicationRepository;
    private final TaskRepository taskRepository;
    private final TaskPriceHistoryRepository priceHistoryRepository;
    private final Clock clock;

    public TaskPriceNegotiationService(TaskApplicationRepository applicationRepository, TaskRepository taskRepository,
            TaskPriceHistoryRepository priceHistoryRepository, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.taskRepository = taskRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.clock = clock;
    }

    /**
     * Tao 1 de xuat gia moi. change_type tu dong theo task.status hien tai (dac ta muc 3).
     * Insert task_price_history NGAY luc de xuat, khong doi den luc duoc dong y (dac ta muc 3
     * va 12). Dieu kien "khong co de xuat PROPOSED khac dang treo" da duoc ChatService kiem
     * tra TRUOC khi goi toi day (xem Javadoc class).
     */
    @Transactional
    public PriceProposalCreated proposePrice(UUID applicationId, UUID proposerAccountId, long amount, String note) {
        TaskApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        Task task = taskRepository.findById(application.getTaskId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        ChangeType changeType = resolveChangeType(task.getStatus());
        TaskPriceHistory history = TaskPriceHistory.propose(UUID.randomUUID(), applicationId, changeType, amount,
                note, proposerAccountId, clock.instant());
        priceHistoryRepository.save(history);
        return new PriceProposalCreated(history.getId(), changeType);
    }

    /**
     * change_type tu dong theo trang thai task hien tai (dac ta muc 3): OPEN (chua ASSIGNED)
     * -> INITIAL_AGREEMENT, ASSIGNED (chua COMPLETED) -> SCOPE_CHANGE. Trang thai khac (da
     * COMPLETED/CLOSED/CANCELLED/REJECTED/con PENDING_REVIEW) khong cho de xuat gia.
     */
    private ChangeType resolveChangeType(TaskStatus taskStatus) {
        if (taskStatus == TaskStatus.OPEN) {
            return ChangeType.INITIAL_AGREEMENT;
        }
        if (taskStatus == TaskStatus.ASSIGNED) {
            return ChangeType.SCOPE_CHANGE;
        }
        throw new BusinessException(ErrorCode.TASK_NOT_NEGOTIABLE);
    }

    /**
     * Ghi nhan 1 de xuat gia duoc Dong y - cap nhat accepted_by_account_id/accepted_at (1 lan
     * duy nhat, xem TaskPriceHistory.accept()), tu dong nang INQUIRING len PENDING neu can
     * (dac ta muc 3), cap nhat application.proposed_price. Dieu kien "ai duoc phep Dong y"
     * (khong phai chinh nguoi tao de xuat) da duoc ChatService kiem tra truoc khi goi toi day.
     * Tra ve amount da chot de Chat dung trong SYSTEM message.
     */
    @Transactional
    public long acceptPriceProposal(UUID applicationId, UUID priceHistoryId, UUID accepterAccountId) {
        TaskPriceHistory history = priceHistoryRepository.findById(priceHistoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        history.accept(accepterAccountId, clock.instant());
        TaskApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        if (application.getStatus() == TaskApplicationStatus.INQUIRING) {
            application.upgradeToPendingFromInquiring();
        }
        application.updateProposedPrice(history.getAmount());
        return history.getAmount();
    }

    /** Doc amount cua 1 dong lich su gia theo id - dung khi Chat can hien lai de xuat cu (listMessages/listInbox). */
    @Transactional(readOnly = true)
    public Optional<Long> findPriceHistoryAmount(UUID priceHistoryId) {
        return priceHistoryRepository.findById(priceHistoryId).map(TaskPriceHistory::getAmount);
    }
}
