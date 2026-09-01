package vn.taskconnect.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.user.dto.request.CreateAvailabilityRequest;
import vn.taskconnect.user.dto.request.UpdateAvailabilityRequest;
import vn.taskconnect.user.entity.TaskerAvailability;
import vn.taskconnect.user.repository.TaskerAvailabilityRepository;

/**
 * Unit test thuan tuy (khong DB) cho TaskerAvailabilityService.
 */
class TaskerAvailabilityServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    private final TaskerAvailabilityRepository repository = mock(TaskerAvailabilityRepository.class);
    private final TaskerAvailabilityService service = new TaskerAvailabilityService(repository);

    @Test
    void should_createSlot_when_endTimeAfterStartTime() {
        when(repository.save(any(TaskerAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskerAvailability result = service.addSlot(ACCOUNT_ID,
                new CreateAvailabilityRequest(1, LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getDayOfWeek()).isEqualTo(1);
        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(result.getEndTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void should_throwValidationFailed_when_endTimeNotAfterStartTime() {
        assertThatThrownBy(() -> service.addSlot(ACCOUNT_ID,
                new CreateAvailabilityRequest(1, LocalTime.of(12, 0), LocalTime.of(12, 0))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
        verify(repository, never()).save(any());
    }

    @Test
    void should_returnSlots_inRepositoryOrder() {
        TaskerAvailability slot = new TaskerAvailability(UUID.randomUUID(), ACCOUNT_ID, 2, LocalTime.of(9, 0),
                LocalTime.of(17, 0));
        when(repository.findByAccountIdOrderByDayOfWeekAscStartTimeAsc(ACCOUNT_ID)).thenReturn(List.of(slot));

        List<TaskerAvailability> result = service.getMySlots(ACCOUNT_ID);

        assertThat(result).containsExactly(slot);
    }

    @Test
    void should_keepOtherFields_when_patchOnlySendsEndTime() {
        TaskerAvailability existing = new TaskerAvailability(UUID.randomUUID(), ACCOUNT_ID, 3, LocalTime.of(8, 0),
                LocalTime.of(12, 0));
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.save(any(TaskerAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskerAvailability result = service.updateSlot(ACCOUNT_ID, existing.getId(),
                new UpdateAvailabilityRequest(null, null, LocalTime.of(13, 0)));

        assertThat(result.getDayOfWeek()).isEqualTo(3);
        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(result.getEndTime()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    void should_throwAvailabilitySlotNotFound_when_updatingSlotOfAnotherAccount() {
        TaskerAvailability othersSlot = new TaskerAvailability(UUID.randomUUID(), UUID.randomUUID(), 3,
                LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(repository.findById(othersSlot.getId())).thenReturn(Optional.of(othersSlot));

        assertThatThrownBy(() -> service.updateSlot(ACCOUNT_ID, othersSlot.getId(),
                new UpdateAvailabilityRequest(null, null, LocalTime.of(13, 0))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AVAILABILITY_SLOT_NOT_FOUND);
    }

    @Test
    void should_throwValidationFailed_when_patchMakesEndTimeNotAfterStartTime() {
        TaskerAvailability existing = new TaskerAvailability(UUID.randomUUID(), ACCOUNT_ID, 3, LocalTime.of(8, 0),
                LocalTime.of(12, 0));
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateSlot(ACCOUNT_ID, existing.getId(),
                new UpdateAvailabilityRequest(null, LocalTime.of(13, 0), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void should_deleteSlot_when_ownedByCaller() {
        TaskerAvailability existing = new TaskerAvailability(UUID.randomUUID(), ACCOUNT_ID, 3, LocalTime.of(8, 0),
                LocalTime.of(12, 0));
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        service.deleteSlot(ACCOUNT_ID, existing.getId());

        verify(repository, times(1)).delete(existing);
    }

    @Test
    void should_throwAvailabilitySlotNotFound_when_deletingSlotOfAnotherAccount() {
        TaskerAvailability othersSlot = new TaskerAvailability(UUID.randomUUID(), UUID.randomUUID(), 3,
                LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(repository.findById(othersSlot.getId())).thenReturn(Optional.of(othersSlot));

        assertThatThrownBy(() -> service.deleteSlot(ACCOUNT_ID, othersSlot.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AVAILABILITY_SLOT_NOT_FOUND);
        verify(repository, never()).delete(any());
    }

    @Test
    void should_throwAvailabilitySlotOverlap_when_newSlotOverlapsExistingOnSameDay() {
        TaskerAvailability existing = new TaskerAvailability(UUID.randomUUID(), ACCOUNT_ID, 1, LocalTime.of(8, 0),
                LocalTime.of(12, 0));
        when(repository.findByAccountIdAndDayOfWeek(ACCOUNT_ID, 1)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.addSlot(ACCOUNT_ID,
                new CreateAvailabilityRequest(1, LocalTime.of(11, 0), LocalTime.of(13, 0))))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.AVAILABILITY_SLOT_OVERLAP);
        verify(repository, never()).save(any());
    }

    @Test
    void should_allowAddingSlot_when_backToBackWithExistingSlotOnSameDay() {
        TaskerAvailability existing = new TaskerAvailability(UUID.randomUUID(), ACCOUNT_ID, 1, LocalTime.of(8, 0),
                LocalTime.of(12, 0));
        when(repository.findByAccountIdAndDayOfWeek(ACCOUNT_ID, 1)).thenReturn(List.of(existing));
        when(repository.save(any(TaskerAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskerAvailability result = service.addSlot(ACCOUNT_ID,
                new CreateAvailabilityRequest(1, LocalTime.of(12, 0), LocalTime.of(17, 0)));

        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void should_allowUpdatingSlot_when_onlyOverlappingWithItself() {
        TaskerAvailability existing = new TaskerAvailability(UUID.randomUUID(), ACCOUNT_ID, 1, LocalTime.of(8, 0),
                LocalTime.of(12, 0));
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.findByAccountIdAndDayOfWeek(ACCOUNT_ID, 1)).thenReturn(List.of(existing));
        when(repository.save(any(TaskerAvailability.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskerAvailability result = service.updateSlot(ACCOUNT_ID, existing.getId(),
                new UpdateAvailabilityRequest(null, LocalTime.of(9, 0), null));

        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(9, 0));
    }
}
