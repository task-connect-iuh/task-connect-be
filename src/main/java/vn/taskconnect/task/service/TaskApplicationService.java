package vn.taskconnect.task.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.admin.api.AdminFacade;
import vn.taskconnect.booking.api.BookingFacade;
import vn.taskconnect.booking.api.dto.BookingSummary;
import vn.taskconnect.chat.api.ChatFacade;
import vn.taskconnect.chat.api.ChatMessageType;
import vn.taskconnect.chat.api.ChatSystemMessages;
import vn.taskconnect.chat.api.ProposalStatus;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.dto.request.ApplyToTaskRequest;
import vn.taskconnect.task.dto.request.InquireRequest;
import vn.taskconnect.task.dto.request.InviteTaskerRequest;
import vn.taskconnect.task.dto.response.ConfirmApplicationResponse;
import vn.taskconnect.task.dto.response.MyApplicationResponse;
import vn.taskconnect.task.dto.response.TaskApplicationResponse;
import vn.taskconnect.task.dto.response.TaskFeedItemResponse;
import vn.taskconnect.task.dto.response.TaskPriceHistoryEntryResponse;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.entity.TaskApplication;
import vn.taskconnect.task.entity.TaskImage;
import vn.taskconnect.task.entity.TaskPriceHistory;
import vn.taskconnect.task.repository.TaskApplicationRepository;
import vn.taskconnect.task.repository.TaskImageRepository;
import vn.taskconnect.task.repository.TaskPriceHistoryRepository;
import vn.taskconnect.task.repository.TaskRepository;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.ServiceCategorySummary;
import vn.taskconnect.user.api.dto.UserProfileSummary;

/**
 * Nghiep vu tim/ung tuyen cong viec (UC09/UC10/UC11/UC16) va Poster xac nhan Tasker (UC11 -
 * viet lai hoan toan tu Round B4 theo TaskConnect_Chat_ImplementationSpec.md muc 7, tao
 * booking-lite that qua BookingFacade). Muc don gian: browse chi loc theo trang thai OPEN +
 * danh muc + tu khoa, chua dung Redis Geo/ban kinh (OQ-02 con MO), khong tra diem uy
 * tin/khoang cach (xem TaskFeedItemResponse). Tu Round B2 them phu thuoc ChatFacade (Task ->
 * Chat, xem 10-module-boundary.md) de mo/dong kenh chat dong bo voi vong doi don ung tuyen; tu
 * Round B4 them BookingFacade (tao booking-lite) va AdminFacade (doc ty le phi xem truoc).
 */
@Service
public class TaskApplicationService {

    private final TaskRepository taskRepository;
    private final TaskImageRepository imageRepository;
    private final TaskApplicationRepository applicationRepository;
    private final TaskPriceHistoryRepository priceHistoryRepository;
    private final UserFacade userFacade;
    private final ChatFacade chatFacade;
    private final BookingFacade bookingFacade;
    private final AdminFacade adminFacade;
    private final Clock clock;

    public TaskApplicationService(TaskRepository taskRepository, TaskImageRepository imageRepository,
            TaskApplicationRepository applicationRepository, TaskPriceHistoryRepository priceHistoryRepository,
            UserFacade userFacade, ChatFacade chatFacade, BookingFacade bookingFacade, AdminFacade adminFacade,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.imageRepository = imageRepository;
        this.applicationRepository = applicationRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.userFacade = userFacade;
        this.chatFacade = chatFacade;
        this.bookingFacade = bookingFacade;
        this.adminFacade = adminFacade;
        this.clock = clock;
    }

    /**
     * Feed cong viec dang OPEN cho Tasker duyet, loc tuy chon theo danh muc va tu khoa (khop
     * khong phan biet hoa/thuong trong title). Khong sap theo khoang cach (xem Javadoc class).
     * Loai tru viec do chinh taskerId dang goi tu dang (1 tai khoan co the mang ca 2 vai tro
     * Poster/Tasker, xem 01-domain-glossary.md) - khong ai duoc thay/ung tuyen viec cua chinh
     * minh trong feed, dung ca cho apply() (CANNOT_APPLY_OWN_TASK). Tu 2026-09-19 loai them viec
     * ma taskerId dang co don o BLOCKING_APPLICATION_STATUSES (da ung tuyen/dang cho phan hoi,
     * hoac tung bi DECLINED/REJECTED) - dung chinh dinh nghia voi requireNoBlockingApplication
     * de feed nhat quan voi rule "khong the ung tuyen lai".
     */
    @Transactional(readOnly = true)
    public List<TaskFeedItemResponse> browseOpenTasks(UUID taskerId, UUID categoryId, String keyword) {
        List<Task> tasks = categoryId != null
                ? taskRepository.findByStatusAndCategoryIdOrderByCreatedAtDesc(TaskStatus.OPEN, categoryId)
                : taskRepository.findByStatusOrderByCreatedAtDesc(TaskStatus.OPEN);
        Set<UUID> blockedTaskIds = applicationRepository
                .findByTaskerIdAndStatusIn(taskerId, BLOCKING_APPLICATION_STATUSES).stream()
                .map(TaskApplication::getTaskId).collect(Collectors.toSet());
        tasks = tasks.stream()
                .filter(task -> !task.getPosterId().equals(taskerId) && !blockedTaskIds.contains(task.getId()))
                .toList();
        if (keyword != null && !keyword.isBlank()) {
            String needle = keyword.trim().toLowerCase(Locale.ROOT);
            tasks = tasks.stream().filter(task -> task.getTitle().toLowerCase(Locale.ROOT).contains(needle)).toList();
        }
        if (tasks.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> categoryNameById = activeCategoryNameById();
        Map<UUID, List<String>> imageUrlsByTaskId = imageUrlsByTaskId(tasks.stream().map(Task::getId).toList());
        return tasks.stream().map(task -> toFeedItem(task, categoryNameById.get(task.getCategoryId()),
                imageUrlsByTaskId.getOrDefault(task.getId(), List.of()))).toList();
    }

    /**
     * Chi tiet 1 cong viec dang OPEN cho Tasker xem - dung khi vao thang URL /tim-viec/{id}.
     * Viec cua chinh taskerId dang goi, hoac viec taskerId dang bi chan boi
     * BLOCKING_APPLICATION_STATUSES, coi nhu khong tim thay, dong bo voi viec bi loai khoi feed
     * (xem Javadoc browseOpenTasks).
     */
    @Transactional(readOnly = true)
    public TaskFeedItemResponse getOpenTaskForBrowse(UUID taskerId, UUID taskId) {
        Task task = taskRepository.findByIdAndStatus(taskId, TaskStatus.OPEN)
                .filter(candidate -> !candidate.getPosterId().equals(taskerId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        if (!applicationRepository.findByTaskIdAndTaskerIdAndStatusIn(taskId, taskerId, BLOCKING_APPLICATION_STATUSES)
                .isEmpty()) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        String categoryName = activeCategoryNameById().get(task.getCategoryId());
        List<String> imageUrls = imageRepository.findByTaskIdOrderByDisplayOrderAsc(taskId).stream()
                .map(TaskImage::getImageUrl).toList();
        return toFeedItem(task, categoryName, imageUrls);
    }

    /**
     * Tasker gui don ung tuyen. Chan: task khong ton tai/khong OPEN (TASK_NOT_OPEN), tu ung
     * tuyen viec cua chinh minh (CANNOT_APPLY_OWN_TASK), dang co don ACTIVE hoac tung bi
     * DECLINED/REJECTED cho cap task+tasker nay (xem requireNoBlockingApplication - tu Round B5
     * sua lai theo dac ta muc 8 ban cap nhat, khong con UNIQUE(task_id, tasker_id) o DB nua, xem
     * V33__drop_unique_task_applications_task_tasker.sql).
     */
    @Transactional
    public TaskApplicationResponse apply(UUID taskerId, UUID taskId, ApplyToTaskRequest request) {
        requireOpenTaskForNewApplication(taskerId, taskId);
        TaskApplication application = TaskApplication.submit(UUID.randomUUID(), taskId, taskerId,
                request.proposedArrivalText(), request.message(), clock.instant());
        applicationRepository.save(application);
        return toApplicationResponse(application);
    }

    /**
     * Tasker bam "Nhan tin hoi them" (UC16 muc 2) - tao don INQUIRING roi TU DONG mo kenh chat
     * kem SYSTEM message va chinh cau hoi cua Tasker lam tin nhan dau tien, CUNG 1 transaction
     * (khac apply()/luong ung tuyen thang: kenh o do chi lazy-create khi thuc su bam Gui trong
     * khung chat, xem dac ta muc 2). Cung dieu kien mo dau nhu apply() (task phai OPEN, khong
     * tu hoi cong viec cua chinh minh, khong dang bi chan boi requireNoBlockingApplication).
     */
    @Transactional
    public TaskApplicationResponse createInquiry(UUID taskerId, UUID taskId, InquireRequest request) {
        requireOpenTaskForNewApplication(taskerId, taskId);
        TaskApplication application = TaskApplication.inquire(UUID.randomUUID(), taskId, taskerId,
                request.message(), clock.instant());
        applicationRepository.save(application);
        String taskerName = resolveDisplayName(taskerId);
        chatFacade.openChannelWithFirstInquiryMessage(application.getId(), taskerId,
                ChatSystemMessages.taskerInquiring(taskerName), request.message(), application.getCreatedAt());
        return toApplicationResponse(application);
    }

    /**
     * Tasker tu rut mot don dang PENDING ("Rut ung tuyen") hoac INQUIRING ("Khong quan tam
     * nua") - dung chung 1 gia tri enum WITHDRAWN, chi khac SYSTEM message theo nguon goc (dac
     * ta muc 5 bang o cuoi). Dong kenh chat neu da co (dac ta muc 6).
     */
    @Transactional
    public TaskApplicationResponse withdraw(UUID taskerId, UUID taskId, UUID applicationId) {
        TaskApplication application = applicationRepository
                .findByIdAndTaskIdAndTaskerId(applicationId, taskId, taskerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        TaskApplicationStatus previousStatus = application.getStatus();
        if (previousStatus != TaskApplicationStatus.PENDING && previousStatus != TaskApplicationStatus.INQUIRING) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_WITHDRAWABLE);
        }
        application.withdraw(clock.instant());
        String taskerName = resolveDisplayName(taskerId);
        String systemMessage = previousStatus == TaskApplicationStatus.PENDING
                ? ChatSystemMessages.taskerWithdrewApplication(taskerName)
                : ChatSystemMessages.taskerStoppedInquiring(taskerName);
        chatFacade.closeChannelIfExists(applicationId, systemMessage);
        return toApplicationResponse(application);
    }

    /**
     * Tasker bam "Ung tuyen" tu 1 don dang INQUIRING (the "Dang hoi them") de chuyen thang
     * thanh PENDING - MO RONG ngoai dac ta muc 3 (ban dau chi nang cap qua acceptPriceProposal
     * khi Dong y de xuat gia trong chat), theo yeu cau nguoi dung 2026-09-21: Tasker duoc chu
     * dong ung tuyen thang tu don hoi them, khong bat buoc phai thuong luong gia truoc (xem
     * PROGRESS-TASK-TASKER-MODULE.md). Chan: don khong thuoc ve taskerId nay hoac khong ton tai
     * (APPLICATION_NOT_FOUND), don khong con INQUIRING (APPLICATION_NOT_INQUIRING), task khong
     * con OPEN - vd da bi giao cho Tasker khac trong luc dang hoi them (TASK_NOT_OPEN). Dung
     * lai upgradeToPendingFromInquiring() da co san tren entity (cung method acceptPriceProposal
     * dang dung), gui SYSTEM message vao kenh chat da mo cua don nay, KHONG dong kenh.
     */
    @Transactional
    public TaskApplicationResponse applyFromInquiry(UUID taskerId, UUID taskId, UUID applicationId) {
        TaskApplication application = applicationRepository
                .findByIdAndTaskIdAndTaskerId(applicationId, taskId, taskerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        if (application.getStatus() != TaskApplicationStatus.INQUIRING) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_INQUIRING);
        }
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }
        application.upgradeToPendingFromInquiring();
        chatFacade.postSystemMessageIfOpen(applicationId,
                ChatSystemMessages.taskerAppliedFromInquiry(resolveDisplayName(taskerId)));
        return toApplicationResponse(application);
    }

    /**
     * Poster moi truc tiep 1 Tasker nhan cong viec (UC09, Round B5, sua lai 2026-09-17 theo
     * TaskConnect_Chat_ImplementationSpec.md muc 8/8.1 ban cap nhat) - chong spam bang 4 gate:
     * Tasker phai dang bat accepts_direct_invites cho dung category cua task
     * (DIRECT_INVITES_DISABLED); khong dang bi chan boi requireNoBlockingApplication (dang co
     * don ACTIVE, hoac tung DECLINED/REJECTED cho task nay); so loi moi INVITED dang cho cua
     * task chua vuot nguong (INVITE_LIMIT_REACHED, doc tu AdminFacade). Moi lai sau khi don cu
     * da WITHDRAWN/REJECTED_AUTO/INVITE_EXPIRED tao MOT DONG MOI HOAN TOAN (application_id moi,
     * kenh chat moi) - khac han thiet ke Round B5 ban dau (tai su dung dong/kenh cu qua
     * reinvite()/reopen(), da bi go bo, xem docs/PROGRESS-CHAT-MODULE.md). Gia de nghi kem theo
     * (neu co) di qua kenh chat nhu 1 PRICE_PROPOSAL, khong luu thang vao proposedPrice (xem
     * Javadoc TaskApplication.invite()).
     */
    @Transactional
    public TaskApplicationResponse invite(UUID posterId, UUID taskId, InviteTaskerRequest request) {
        Task task = requireOwnedTask(posterId, taskId);
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }
        UUID taskerId = request.taskerId();
        if (taskerId.equals(posterId)) {
            throw new BusinessException(ErrorCode.CANNOT_APPLY_OWN_TASK);
        }
        if (!userFacade.acceptsDirectInvites(taskerId, task.getCategoryId())) {
            throw new BusinessException(ErrorCode.DIRECT_INVITES_DISABLED);
        }
        requireNoBlockingApplication(taskId, taskerId);
        if (applicationRepository.countByTaskIdAndStatus(taskId, TaskApplicationStatus.INVITED)
                >= adminFacade.getMaxConcurrentInvitesPerTask()) {
            throw new BusinessException(ErrorCode.INVITE_LIMIT_REACHED);
        }

        Instant now = clock.instant();
        Instant expiresAt = now.plus(Duration.ofHours(adminFacade.getInviteExpiryHours()));
        TaskApplication application = TaskApplication.invite(UUID.randomUUID(), taskId, taskerId, request.message(),
                now, expiresAt);
        applicationRepository.save(application);

        String posterName = resolveDisplayName(posterId);
        chatFacade.openChannelForInvite(application.getId(), posterId, ChatSystemMessages.posterInvited(posterName),
                request.proposedPrice(), request.message(), now);
        return toApplicationResponse(application);
    }

    /**
     * Tasker nhan mot loi moi truc tiep dang INVITED - chuyen PENDING, xoa han het han. Phong
     * ho truong hop vua het han ma tien trinh nen (InviteExpirySweeperJob) chua kip quet toi
     * (dac ta muc 8): neu da qua expires_at, tu chuyen INVITE_EXPIRED ngay va bao loi thay vi
     * cho nhan thanh cong mot loi moi thuc ra da het han.
     */
    @Transactional
    public TaskApplicationResponse acceptInvite(UUID taskerId, UUID taskId, UUID applicationId) {
        TaskApplication application = applicationRepository
                .findByIdAndTaskIdAndTaskerId(applicationId, taskId, taskerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        requireInviteStillValid(application);
        application.acceptInvite(clock.instant());
        return toApplicationResponse(application);
    }

    /** Tasker tu choi mot loi moi truc tiep dang INVITED - chan moi lai vinh vien (TASKER_DECLINED_PERMANENTLY), dong kenh chat. */
    @Transactional
    public TaskApplicationResponse declineInvite(UUID taskerId, UUID taskId, UUID applicationId) {
        TaskApplication application = applicationRepository
                .findByIdAndTaskIdAndTaskerId(applicationId, taskId, taskerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        requireInviteStillValid(application);
        application.declineInvite(clock.instant());
        chatFacade.closeChannelIfExists(applicationId,
                ChatSystemMessages.taskerDeclinedInvite(resolveDisplayName(taskerId)));
        return toApplicationResponse(application);
    }

    /**
     * Gate chung cho acceptInvite/declineInvite: don phai dang INVITED (APPLICATION_NOT_PENDING
     * neu khac) va chua qua han. Neu da qua han ma tien trinh nen (InviteExpirySweeperJob) chua
     * kip quet toi, CHI bao loi INVITE_EXPIRED - KHONG tu sua trang thai/dong kenh ngay tai day:
     * lam vay se bi ROLLBACK ngay lap tuc cung voi BusinessException nem ra sau do (ca hai nam
     * chung 1 @Transactional cua acceptInvite/declineInvite, khong phai loi nghiep vu ma la loi
     * da phat hien khi smoke test round B5 - xem docs/PROGRESS-CHAT-MODULE.md). Viec tu sua
     * trang thai that su thuoc ve sweepExpiredInvites() (chay dinh ky, transaction rieng).
     */
    private void requireInviteStillValid(TaskApplication application) {
        if (application.getStatus() != TaskApplicationStatus.INVITED) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_PENDING);
        }
        if (application.getExpiresAt() != null && !application.getExpiresAt().isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.INVITE_EXPIRED);
        }
    }

    /**
     * Quet toan bo loi moi INVITED da qua expires_at, chuyen INVITE_EXPIRED + dong kenh chat
     * neu co (dac ta muc 8) - KHONG chan moi lai (khac DECLINED, xem invite()). Chay dinh ky
     * boi InviteExpirySweeperJob (task/infrastructure).
     */
    @Transactional
    public void sweepExpiredInvites() {
        Instant now = clock.instant();
        for (TaskApplication application : applicationRepository
                .findByStatusAndExpiresAtBefore(TaskApplicationStatus.INVITED, now)) {
            application.expireInvite(now);
            chatFacade.closeChannelIfExists(application.getId(), ChatSystemMessages.INVITE_EXPIRED);
        }
    }

    /**
     * Trang thai duoc coi la dang "chan" viec tao 1 don moi (ung tuyen/hoi them/moi) cho cung 1
     * cap task+tasker - dac ta muc 8 ban cap nhat 2026-09-17: ACTIVE (chua ket thuc, chi co the
     * co toi da 1 dong o nhom nay tai 1 thoi diem) hoac da tung bi tu choi dich danh (DECLINED -
     * Tasker tu choi loi moi; REJECTED - Poster tu choi thu cong, quyet dinh nguoi dung
     * 2026-09-17 coi tuong duong DECLINED). Cac trang thai da ket thuc KHAC (WITHDRAWN,
     * REJECTED_AUTO, INVITE_EXPIRED) khong nam trong danh sach nay - khong chan gi ca.
     */
    private static final Set<TaskApplicationStatus> BLOCKING_APPLICATION_STATUSES = EnumSet.of(
            TaskApplicationStatus.PENDING, TaskApplicationStatus.INQUIRING, TaskApplicationStatus.INVITED,
            TaskApplicationStatus.DECLINED, TaskApplicationStatus.REJECTED);

    /**
     * Dieu kien chung truoc khi tao 1 don ung tuyen moi (du la apply() hay createInquiry()):
     * task phai ton tai va dang OPEN, khong duoc tu ung tuyen/hoi viec cua chinh minh, va khong
     * dang bi chan boi requireNoBlockingApplication.
     */
    private Task requireOpenTaskForNewApplication(UUID taskerId, UUID taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_NOT_OPEN);
        }
        if (task.getPosterId().equals(taskerId)) {
            throw new BusinessException(ErrorCode.CANNOT_APPLY_OWN_TASK);
        }
        requireNoBlockingApplication(taskId, taskerId);
        return task;
    }

    /**
     * Chan tao 1 don moi (ung tuyen/hoi them/moi) cho cap task+tasker nay neu dang co don ACTIVE
     * (PENDING/INQUIRING/INVITED - bao loi ALREADY_APPLIED) hoac tung bi DECLINED/REJECTED (bao
     * loi TASKER_DECLINED_PERMANENTLY - chan vinh vien). Khong con dua vao UNIQUE constraint o
     * DB (da bo, xem V33__drop_unique_task_applications_task_tasker.sql) - kiem tra hoan toan o
     * tang service theo quyet dinh nguoi dung 2026-09-17 (chap nhan rui ro race condition rat
     * nho giua 2 request gan nhu dong thoi, phu hop quy mo do an).
     */
    private void requireNoBlockingApplication(UUID taskId, UUID taskerId) {
        List<TaskApplication> blocking = applicationRepository.findByTaskIdAndTaskerIdAndStatusIn(taskId, taskerId,
                BLOCKING_APPLICATION_STATUSES);
        for (TaskApplication existing : blocking) {
            if (existing.getStatus() == TaskApplicationStatus.DECLINED
                    || existing.getStatus() == TaskApplicationStatus.REJECTED) {
                throw new BusinessException(ErrorCode.TASKER_DECLINED_PERMANENTLY);
            }
        }
        if (!blocking.isEmpty()) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }
    }

    /** Toan bo don ung tuyen (moi trang thai) cua chinh Tasker dang goi - dung cho man "Viec da nhan". */
    @Transactional(readOnly = true)
    public List<MyApplicationResponse> listMyApplications(UUID taskerId) {
        List<TaskApplication> applications = applicationRepository.findByTaskerIdOrderByCreatedAtDesc(taskerId);
        if (applications.isEmpty()) {
            return List.of();
        }
        List<UUID> taskIds = applications.stream().map(TaskApplication::getTaskId).distinct().toList();
        Map<UUID, Task> taskById = taskRepository.findAllById(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, t -> t));
        Map<UUID, String> categoryNameById = activeCategoryNameById();
        Map<UUID, List<String>> imageUrlsByTaskId = imageUrlsByTaskId(taskIds);
        Map<UUID, String> posterNameById = new HashMap<>();
        return applications.stream()
                .filter(app -> taskById.containsKey(app.getTaskId()))
                .map(app -> {
                    Task task = taskById.get(app.getTaskId());
                    String posterName = posterNameById.computeIfAbsent(task.getPosterId(), this::resolveDisplayName);
                    return new MyApplicationResponse(app.getId(), app.getStatus(), app.getInitiatedBy(),
                            app.getExpiresAt(), app.getProposedArrivalText(),
                            app.getMessage(), app.getCreatedAt(), app.getRespondedAt(), task.getId(), task.getTitle(),
                            task.getDescription(), task.getAddressText(), task.getLocationType(),
                            task.getArrivalNotes(), task.getSuppliesStatus(), task.getSuppliesNote(),
                            task.getLat(), task.getLng(), task.getBudgetAmount(), task.getScheduledAt(),
                            task.getStatus(), task.getCategoryId(),
                            categoryNameById.get(task.getCategoryId()), posterName,
                            imageUrlsByTaskId.getOrDefault(task.getId(), List.of()));
                })
                .toList();
    }

    /** Poster xem danh sach ung vien cua 1 cong viec cua chinh minh - 404 kieu TASK_NOT_FOUND neu khong phai chu. */
    @Transactional(readOnly = true)
    public List<TaskApplicationResponse> listApplicantsForOwner(UUID posterId, UUID taskId) {
        requireOwnedTask(posterId, taskId);
        return applicationRepository.findByTaskId(taskId).stream().map(this::toApplicationResponse).toList();
    }

    /**
     * UC11 "Chon nguoi nay" - viet lai hoan toan tu Round B4 theo
     * TaskConnect_Chat_ImplementationSpec.md muc 7 (thay the accept()/markNeedsReconfirm()).
     * Gate: don phai dang PENDING, khong duoc co PRICE_PROPOSAL nao dang PROPOSED chua xu ly.
     * fee_base = gia da thuong luong (application.proposedPrice) neu co, else ngan sach goc
     * cua task (dac ta muc 4) - neu ca 2 deu null (task "thoa thuan" ma chua ai thuong luong
     * gia lan nao) thi khong the tao booking. Ung vien thang GIU NGUYEN PENDING (khong con goi
     * accept() - da chot voi nguoi dung khi lap ke hoach, dau hieu "da thang" la co booking
     * gan voi application). Moi don PENDING/INQUIRING/INVITED con lai cua task chuyen
     * REJECTED_AUTO (thay markNeedsReconfirm()) + dong kenh kem SYSTEM message chuan. Them
     * 2026-09-21: kenh cua chinh ung vien THANG cung nhan 1 SYSTEM message rieng
     * (taskerConfirmed(), lazy-create kenh neu chua tung chat gi) - truoc day chi cac ung vien
     * THUA moi co thong bao, ben thang khong biet gi ca cho toi khi tu load lai (xem
     * docs/PROGRESS-CHAT-MODULE.md). Kenh nay KHONG dong (con "Dang thuc hien").
     */
    @Transactional
    public ConfirmApplicationResponse confirm(UUID posterId, UUID taskId, UUID applicationId) {
        Task task = requireOwnedTask(posterId, taskId);
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_ASSIGNED);
        }
        TaskApplication target = applicationRepository.findByIdAndTaskId(applicationId, taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        if (target.getStatus() != TaskApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_PENDING);
        }
        if (chatFacade.hasPendingProposal(applicationId, ChatMessageType.PRICE_PROPOSAL)) {
            throw new BusinessException(ErrorCode.PRICE_PROPOSAL_PENDING);
        }
        Long feeBase = target.getProposedPrice() != null ? target.getProposedPrice() : task.getBudgetAmount();
        if (feeBase == null) {
            throw new BusinessException(ErrorCode.MISSING_AGREED_PRICE);
        }

        Instant now = clock.instant();
        BookingSummary booking = bookingFacade.createFromApplication(applicationId, taskId, posterId,
                target.getTaskerId(), feeBase, task.getScheduledAt());
        task.assignTo(now);
        // THU TU QUAN TRONG (sua 2026-09-22): notifyApplicationConfirmed() chay TRUOC vi no
        // lazy-create kenh neu ung vien thang chua tung chat/de xuat gi truoc do (vd task co san
        // ngan sach, Poster "Chon nguoi nay" ma khong can thuong luong - rat pho bien sau fix
        // 2026-09-21 cho phep fallback ngan sach). attachBooking() CHI gan bookingId vao kenh DA
        // TON TAI (channelRepository.findByApplicationId(...).ifPresent(...), no-op neu chua co
        // kenh) - goi truoc notifyApplicationConfirmed() nhu ban cu se lam bookingId khong bao
        // gio duoc gan cho nhung ung vien thang chua tung co kenh, khien tab "Dang thuc hien"
        // (dua tren hasBooking, xem docs/PROGRESS-CHAT-MODULE.md 2026-09-22) luon rong.
        chatFacade.notifyApplicationConfirmed(applicationId,
                ChatSystemMessages.taskerConfirmed(resolveDisplayName(posterId)), now);
        chatFacade.attachBooking(applicationId, booking.id());

        for (TaskApplication other : applicationRepository.findByTaskId(taskId)) {
            if (!other.getId().equals(target.getId()) && isStillActiveForCascade(other.getStatus())) {
                other.rejectAuto(now);
                chatFacade.closeChannelIfExists(other.getId(), ChatSystemMessages.TASK_ASSIGNED_TO_ANOTHER);
            }
        }

        long platformFee = computePlatformFee(feeBase);
        long payoutEstimate = feeBase - platformFee;
        return new ConfirmApplicationResponse(toApplicationResponse(target), booking.id(), feeBase, platformFee,
                payoutEstimate);
    }

    /** Cac trang thai con "song" bi cuon vao REJECTED_AUTO khi UC11 chon xong nguoi thang (dac ta muc 5 so do). */
    private boolean isStillActiveForCascade(TaskApplicationStatus status) {
        return status == TaskApplicationStatus.PENDING || status == TaskApplicationStatus.INQUIRING
                || status == TaskApplicationStatus.INVITED;
    }

    /**
     * Phi nen tang xem truoc = fee_base x ty le (doc tu AdminFacade, khong hardcode), lam tron
     * XUONG ve dong nguyen - phan du thuoc ve Tasker (.claude/rules/14-payment-escrow.md). Chi
     * la so lieu hien thi o dot nay (chua co Payment/escrow that giu tien).
     */
    private long computePlatformFee(long feeBase) {
        BigDecimal feeRate = adminFacade.getPlatformFeeRate();
        return BigDecimal.valueOf(feeBase).multiply(feeRate).setScale(0, RoundingMode.DOWN).longValueExact();
    }

    /**
     * Poster tu choi thu cong mot ung vien - chuyen don do sang REJECTED, khong dong den cac
     * don khac. Hanh dong nay GIU LAI ngoai dac ta Chat theo yeu cau nguoi dung (khac
     * REJECTED_AUTO - cascade tu dong cua he thong khi UC11 chon xong nguoi thang, Round B4) -
     * dong kenh chat neu da co, dung sat tinh than dac ta muc 6 (mo rong them REJECTED vao
     * danh sach trang thai dong kenh, xem docs/PROGRESS-CHAT-MODULE.md).
     */
    @Transactional
    public TaskApplicationResponse reject(UUID posterId, UUID taskId, UUID applicationId) {
        requireOwnedTask(posterId, taskId);
        TaskApplication target = applicationRepository.findByIdAndTaskId(applicationId, taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        if (target.getStatus() != TaskApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_PENDING);
        }
        target.reject(clock.instant());
        chatFacade.closeChannelIfExists(applicationId, ChatSystemMessages.posterRejectedManually(
                resolveDisplayName(posterId)));
        return toApplicationResponse(target);
    }

    /** Kiem tra task ton tai va thuoc ve dung Poster - gop 403/404 thanh TASK_NOT_FOUND, cung pattern voi TaskService.getTaskForOwner. */
    private Task requireOwnedTask(UUID posterId, UUID taskId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getPosterId().equals(posterId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    private TaskFeedItemResponse toFeedItem(Task task, String categoryName, List<String> imageUrls) {
        UserProfileSummary poster = userFacade.findProfile(task.getPosterId()).orElse(null);
        return new TaskFeedItemResponse(task.getId(), task.getCategoryId(), categoryName, task.getTitle(),
                task.getDescription(), task.getAddressText(), task.getLat(), task.getLng(), task.getLocationType(),
                task.getArrivalNotes(), task.getSuppliesStatus(), task.getSuppliesNote(), task.getBudgetAmount(),
                task.getScheduledAt(), imageUrls, task.getPosterId(), poster != null ? poster.fullName() : null,
                poster != null ? poster.avatarUrl() : null, task.getCreatedAt());
    }

    private TaskApplicationResponse toApplicationResponse(TaskApplication application) {
        String taskerName = resolveDisplayName(application.getTaskerId());
        String taskerAvatarUrl = userFacade.findProfile(application.getTaskerId()).map(UserProfileSummary::avatarUrl)
                .orElse(null);
        PriceSnapshot price = priceSnapshotFor(application.getId());
        return new TaskApplicationResponse(application.getId(), application.getTaskerId(), taskerName,
                taskerAvatarUrl, application.getProposedArrivalText(), application.getMessage(),
                application.getStatus(), application.getInitiatedBy(), application.getExpiresAt(),
                application.getCreatedAt(), application.getRespondedAt(), price.agreedAmount(), price.pendingAmount());
    }

    /** Gia da chot (neu co) va gia dang cho quyet dinh (neu co) cho 1 application - xem Javadoc TaskApplicationResponse. */
    private record PriceSnapshot(Long agreedAmount, Long pendingAmount) {
    }

    /**
     * Doc task_price_history (module nay so huu) de suy ra 2 gia tri hien thi cho 1 application:
     * "gia da chot" la dong ACCEPTED gan nhat (khong bao gio sai vi Dong y la hanh dong khong
     * dao nguoc). "Gia dang cho" chi tin duoc SAU KHI da hoi ChatFacade.hasPendingProposal() -
     * tu rieng bang nay khong the phan biet "dong NULL la dang cho" voi "dong NULL la tan du
     * cua 1 lan Tu choi/Thu hoi truoc do" (ca 2 deu accepted_at NULL, xem Javadoc
     * TaskPriceHistory). Neu dang co dung 1 de xuat PROPOSED (bat buoc theo dac ta), dong moi
     * nhat trong bang chinh la de xuat do.
     */
    private PriceSnapshot priceSnapshotFor(UUID applicationId) {
        Long agreed = priceHistoryRepository
                .findTopByApplicationIdAndAcceptedAtIsNotNullOrderByAcceptedAtDesc(applicationId)
                .map(TaskPriceHistory::getAmount).orElse(null);
        Long pending = chatFacade.hasPendingProposal(applicationId, ChatMessageType.PRICE_PROPOSAL)
                ? priceHistoryRepository.findTopByApplicationIdOrderByCreatedAtDesc(applicationId)
                        .map(TaskPriceHistory::getAmount).orElse(null)
                : null;
        return new PriceSnapshot(agreed, pending);
    }

    /**
     * Toan bo lich su gia (chi ghi them) cua 1 application - ca Poster va Tasker cua don do xem
     * duoc (dung cho man "Lich su gia"), 403 nguoi khac. Khong gioi han theo trang thai task/
     * don - lich su van xem lai duoc ke ca sau khi da dong/huy, dung tinh than "ban ghi lam can
     * cu khi co khieu nai" (dac ta muc 12).
     */
    @Transactional(readOnly = true)
    public List<TaskPriceHistoryEntryResponse> listPriceHistory(UUID requesterAccountId, UUID applicationId) {
        TaskApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        Task task = taskRepository.findById(application.getTaskId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        boolean isParty = requesterAccountId.equals(task.getPosterId())
                || requesterAccountId.equals(application.getTaskerId());
        if (!isParty) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        List<TaskPriceHistory> history = priceHistoryRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
        Map<UUID, ProposalStatus> statusByHistoryId = chatFacade.findProposalStatusesByPriceHistoryIds(
                history.stream().map(TaskPriceHistory::getId).collect(Collectors.toSet()));
        return history.stream()
                .map(entry -> toPriceHistoryEntryResponse(entry, statusByHistoryId.get(entry.getId())))
                .toList();
    }

    /**
     * proposalStatus doc tu ChatFacade.findProposalStatusesByPriceHistoryIds() (xem Javadoc
     * TaskPriceHistoryEntryResponse) - mac dinh PROPOSED neu vi ly do nao do khong tim thay dong
     * chat_messages tuong ung (khong nen xay ra, an toan hon la nem loi 500 cho man lich su).
     */
    private TaskPriceHistoryEntryResponse toPriceHistoryEntryResponse(TaskPriceHistory history,
            ProposalStatus proposalStatus) {
        String acceptedByName = history.getAcceptedByAccountId() != null
                ? resolveDisplayName(history.getAcceptedByAccountId())
                : null;
        return new TaskPriceHistoryEntryResponse(history.getId(), history.getChangeType(), history.getAmount(),
                history.getNote(), history.getCreatedByAccountId(), resolveDisplayName(history.getCreatedByAccountId()),
                history.getCreatedAt(), history.getAcceptedByAccountId(), acceptedByName, history.getAcceptedAt(),
                proposalStatus != null ? proposalStatus : ProposalStatus.PROPOSED);
    }

    private String resolveDisplayName(UUID accountId) {
        return userFacade.findProfile(accountId).map(UserProfileSummary::fullName).orElse(null);
    }

    private Map<UUID, String> activeCategoryNameById() {
        return userFacade.listActiveServiceCategories().stream()
                .collect(Collectors.toMap(ServiceCategorySummary::id, ServiceCategorySummary::name));
    }

    private Map<UUID, List<String>> imageUrlsByTaskId(List<UUID> taskIds) {
        return imageRepository.findByTaskIdInOrderByDisplayOrderAsc(taskIds).stream()
                .collect(Collectors.groupingBy(TaskImage::getTaskId,
                        Collectors.mapping(TaskImage::getImageUrl, Collectors.toList())));
    }
}
