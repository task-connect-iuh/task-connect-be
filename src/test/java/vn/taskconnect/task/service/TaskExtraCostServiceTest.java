package vn.taskconnect.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.taskconnect.booking.api.BookingFacade;
import vn.taskconnect.booking.api.PaymentMethod;
import vn.taskconnect.booking.api.dto.BookingEscrowSummary;
import vn.taskconnect.chat.api.ChatFacade;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.payment.api.EscrowHoldStatus;
import vn.taskconnect.task.api.ExtraCostBatchStatus;
import vn.taskconnect.task.dto.request.ExtraCostItemInput;
import vn.taskconnect.task.dto.response.ExtraCostMoneySummaryResponse;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.task.entity.TaskApplication;
import vn.taskconnect.task.entity.TaskExtraCostBatch;
import vn.taskconnect.task.repository.TaskApplicationRepository;
import vn.taskconnect.task.repository.TaskExtraCostBatchRepository;
import vn.taskconnect.task.repository.TaskExtraCostItemRepository;
import vn.taskconnect.task.repository.TaskRepository;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.LocationType;
import vn.taskconnect.task.api.SuppliesStatus;

/**
 * Unit test thuan tuy cho TaskExtraCostService.submit() - trong tam vao 3 gate nghiep vu
 * (ASSIGNED, FULL_ESCROW, khong co batch PENDING khac) vi day la dieu kien duy nhat quyet dinh
 * Tasker co dang duoc chi phi phat sinh hay khong, xem docs/PROGRESS-TASK-TASKER-MODULE.md.
 */
class TaskExtraCostServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-02T10:00:00Z");
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();
    private static final UUID POSTER_ID = UUID.randomUUID();
    private static final UUID TASKER_ID = UUID.randomUUID();

    private final TaskApplicationRepository applicationRepository = mock(TaskApplicationRepository.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final TaskExtraCostBatchRepository batchRepository = mock(TaskExtraCostBatchRepository.class);
    private final TaskExtraCostItemRepository itemRepository = mock(TaskExtraCostItemRepository.class);
    private final BookingFacade bookingFacade = mock(BookingFacade.class);
    private final ChatFacade chatFacade = mock(ChatFacade.class);
    private final UserFacade userFacade = mock(UserFacade.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final TaskExtraCostService service = new TaskExtraCostService(
            applicationRepository, taskRepository, batchRepository, itemRepository, bookingFacade, chatFacade,
            userFacade, clock);

    private static Task openTask() {
        return Task.createOpen(TASK_ID, POSTER_ID, UUID.randomUUID(), "Sửa ống nước", "Mô tả", "123 Lê Lợi",
                BigDecimal.valueOf(10.77), BigDecimal.valueOf(106.7), LocationType.NHA_RIENG, null,
                SuppliesStatus.FULL, null, 500_000L, FIXED_NOW, 1, FIXED_NOW);
    }

    private static TaskApplication applicationOf(UUID taskerId) {
        return TaskApplication.submit(APPLICATION_ID, TASK_ID, taskerId, null, null, FIXED_NOW);
    }

    private static List<ExtraCostItemInput> oneItem() {
        return List.of(new ExtraCostItemInput("Cụm vòi rửa gắn tường mới", 180_000L, null));
    }

    @Test
    void should_throwTaskNotAssigned_when_taskStillOpen() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(applicationOf(TASKER_ID)));
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(openTask()));

        assertThatThrownBy(() -> service.submit(TASKER_ID, APPLICATION_ID, null, oneItem()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TASK_NOT_ASSIGNED_FOR_EXTRA_COST);
    }

    @Test
    void should_throwNotAllowedForPaymentMethod_when_bookingIsFeeOnlyEscrow() {
        Task task = openTask();
        task.assignTo(FIXED_NOW);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(applicationOf(TASKER_ID)));
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(bookingFacade.getEscrowBreakdown(APPLICATION_ID)).thenReturn(Optional.of(
                new BookingEscrowSummary(UUID.randomUUID(), 500_000L, PaymentMethod.FEE_ONLY_ESCROW, 40_000L,
                        EscrowHoldStatus.HELD)));

        assertThatThrownBy(() -> service.submit(TASKER_ID, APPLICATION_ID, null, oneItem()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTRA_COST_NOT_ALLOWED_FOR_PAYMENT_METHOD);
    }

    @Test
    void should_throwBatchPending_when_anotherBatchAlreadyPending() {
        Task task = openTask();
        task.assignTo(FIXED_NOW);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(applicationOf(TASKER_ID)));
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(bookingFacade.getEscrowBreakdown(APPLICATION_ID)).thenReturn(Optional.of(
                new BookingEscrowSummary(UUID.randomUUID(), 500_000L, PaymentMethod.FULL_ESCROW, 500_000L,
                        EscrowHoldStatus.HELD)));
        when(batchRepository.existsByApplicationIdAndStatus(APPLICATION_ID, ExtraCostBatchStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(TASKER_ID, APPLICATION_ID, null, oneItem()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTRA_COST_BATCH_PENDING);
    }

    @Test
    void should_saveBatchWithSummedTotal_when_allGatesPass() {
        Task task = openTask();
        task.assignTo(FIXED_NOW);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(applicationOf(TASKER_ID)));
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(bookingFacade.getEscrowBreakdown(APPLICATION_ID)).thenReturn(Optional.of(
                new BookingEscrowSummary(UUID.randomUUID(), 500_000L, PaymentMethod.FULL_ESCROW, 500_000L,
                        EscrowHoldStatus.HELD)));
        when(batchRepository.existsByApplicationIdAndStatus(APPLICATION_ID, ExtraCostBatchStatus.PENDING)).thenReturn(false);
        when(batchRepository.countByApplicationId(APPLICATION_ID)).thenReturn(0);
        when(batchRepository.save(any(TaskExtraCostBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(batchRepository.findByApplicationIdOrderByCreatedAtAsc(APPLICATION_ID)).thenReturn(List.of());
        when(itemRepository.findByBatchIdInOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(userFacade.findProfile(any())).thenReturn(Optional.empty());

        List<ExtraCostItemInput> items = List.of(
                new ExtraCostItemInput("Cụm vòi rửa gắn tường mới", 180_000L, null),
                new ExtraCostItemInput("Công tháo đục ren cũ", 40_000L, null));
        ExtraCostMoneySummaryResponse result = service.submit(TASKER_ID, APPLICATION_ID, "Vòi cũ mục", items);

        ArgumentCaptor<TaskExtraCostBatch> batchCaptor = ArgumentCaptor.forClass(TaskExtraCostBatch.class);
        verify(batchRepository).save(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getTotalAmount()).isEqualTo(220_000L);
        assertThat(batchCaptor.getValue().getBatchNo()).isEqualTo(1);
        assertThat(batchCaptor.getValue().getStatus()).isEqualTo(ExtraCostBatchStatus.PENDING);
        verify(itemRepository).saveAll(any());
        verify(chatFacade).postExtraCostBatchMessageIfOpen(eq(APPLICATION_ID), eq(TASKER_ID), any());
        assertThat(result.requiredTotal()).isEqualTo(500_000L);
    }
}
