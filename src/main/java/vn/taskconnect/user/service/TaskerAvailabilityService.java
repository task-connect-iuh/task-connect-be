package vn.taskconnect.user.service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.user.dto.request.CreateAvailabilityRequest;
import vn.taskconnect.user.dto.request.UpdateAvailabilityRequest;
import vn.taskconnect.user.entity.TaskerAvailability;
import vn.taskconnect.user.repository.TaskerAvailabilityRepository;

/**
 * Nghiep vu lich ranh Tasker (Buoc 7) - khai bao, xem, sua, xoa khung gio ranh trong tuan.
 * Doc lap voi cac buoc con lai cua module User (khong can KYC hay ky nang da xac minh),
 * chi la thong tin tu khai, khong qua duyet. Khong ho tro khung gio qua nua dem (vd
 * 22:00-02:00) o Buoc nay - endTime luon phai sau startTime trong cung mot ngay, chap nhan
 * cho MVP.
 */
@Service
public class TaskerAvailabilityService {

    private final TaskerAvailabilityRepository availabilityRepository;

    public TaskerAvailabilityService(TaskerAvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    /** Khai bao mot khung gio ranh moi - khong co UNIQUE tren (account_id, day_of_week), cho phep nhieu khung/ngay nhung khong duoc trung gio nhau. */
    @Transactional
    public TaskerAvailability addSlot(UUID accountId, CreateAvailabilityRequest request) {
        requireEndAfterStart(request.startTime(), request.endTime());
        requireNoOverlap(accountId, request.dayOfWeek(), request.startTime(), request.endTime(), null);
        TaskerAvailability slot = new TaskerAvailability(UUID.randomUUID(), accountId, request.dayOfWeek(),
                request.startTime(), request.endTime());
        return availabilityRepository.save(slot);
    }

    /** Toan bo khung gio ranh cua chinh chu tai khoan, sap theo thu roi gio bat dau. */
    @Transactional(readOnly = true)
    public List<TaskerAvailability> getMySlots(UUID accountId) {
        return availabilityRepository.findByAccountIdOrderByDayOfWeekAscStartTimeAsc(accountId);
    }

    /**
     * Sua mot phan khung gio da khai bao - truong nao null trong request giu nguyen gia
     * tri cu, cung ngu nghia PATCH da dung o UserProfileService.applyPartialUpdate.
     */
    @Transactional
    public TaskerAvailability updateSlot(UUID accountId, UUID slotId, UpdateAvailabilityRequest request) {
        TaskerAvailability slot = requireOwnSlot(accountId, slotId);
        int dayOfWeek = request.dayOfWeek() != null ? request.dayOfWeek() : slot.getDayOfWeek();
        LocalTime startTime = request.startTime() != null ? request.startTime() : slot.getStartTime();
        LocalTime endTime = request.endTime() != null ? request.endTime() : slot.getEndTime();
        requireEndAfterStart(startTime, endTime);
        requireNoOverlap(accountId, dayOfWeek, startTime, endTime, slotId);
        slot.update(dayOfWeek, startTime, endTime);
        return availabilityRepository.save(slot);
    }

    /** Xoa mot khung gio da khai bao. */
    @Transactional
    public void deleteSlot(UUID accountId, UUID slotId) {
        TaskerAvailability slot = requireOwnSlot(accountId, slotId);
        availabilityRepository.delete(slot);
    }

    /**
     * Tim khung gio theo id va bat buoc thuoc ve chinh tai khoan dang goi - khong phan
     * biet "khong ton tai" voi "cua tai khoan khac" trong thong bao loi, tranh lo thong tin
     * qua ma loi (cung nguyen tac kiem tra quyen so huu cua 16-api-contract.md).
     */
    private TaskerAvailability requireOwnSlot(UUID accountId, UUID slotId) {
        TaskerAvailability slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AVAILABILITY_SLOT_NOT_FOUND));
        if (!slot.getAccountId().equals(accountId)) {
            throw new BusinessException(ErrorCode.AVAILABILITY_SLOT_NOT_FOUND);
        }
        return slot;
    }

    /** Gio ket thuc phai sau gio bat dau - khong ho tro khung gio qua nua dem o Buoc nay. */
    private void requireEndAfterStart(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Giờ kết thúc phải sau giờ bắt đầu.");
        }
    }

    /**
     * Khung gio moi/sua khong duoc chong lan voi bat ky khung gio nao khac da khai bao
     * trong CUNG mot ngay cua chinh Tasker do - hai khoang [start1,end1) va [start2,end2)
     * chong lan khi start1 < end2 VA start2 < end1. excludeSlotId dung khi sua (bo qua
     * chinh dong dang sua so voi chinh no), null khi them moi.
     */
    private void requireNoOverlap(UUID accountId, int dayOfWeek, LocalTime startTime, LocalTime endTime,
            UUID excludeSlotId) {
        boolean overlaps = availabilityRepository.findByAccountIdAndDayOfWeek(accountId, dayOfWeek).stream()
                .filter(other -> excludeSlotId == null || !other.getId().equals(excludeSlotId))
                .anyMatch(other -> startTime.isBefore(other.getEndTime()) && other.getStartTime().isBefore(endTime));
        if (overlaps) {
            throw new BusinessException(ErrorCode.AVAILABILITY_SLOT_OVERLAP);
        }
    }
}
