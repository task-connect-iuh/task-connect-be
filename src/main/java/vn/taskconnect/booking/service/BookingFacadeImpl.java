package vn.taskconnect.booking.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.booking.api.BookingFacade;
import vn.taskconnect.booking.api.dto.BookingSummary;
import vn.taskconnect.booking.entity.Booking;
import vn.taskconnect.booking.repository.BookingRepository;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.task.api.TaskFacade;
import vn.taskconnect.task.api.TaskStatus;

/**
 * Trien khai duy nhat cua BookingFacade - khong module nao khac trong booking duoc implements
 * interface nay. Tu Round B6 phu thuoc them TaskFacade (Booking -> Task, da cho phep san trong
 * 10-module-boundary.md) de kiem tra task.status khi xet de xuat doi lich - AN TOAN ve vong
 * tron bean vi TaskFacadeImpl khong phu thuoc nguoc lai BookingFacade (xem quy tac phat hien o
 * Round B3, docs/PROGRESS-CHAT-MODULE.md).
 */
@Service
class BookingFacadeImpl implements BookingFacade {

    private final BookingRepository bookingRepository;
    private final TaskFacade taskFacade;
    private final Clock clock;

    BookingFacadeImpl(BookingRepository bookingRepository, TaskFacade taskFacade, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.taskFacade = taskFacade;
        this.clock = clock;
    }

    /** Doc booking theo applicationId va anh xa sang BookingSummary de tra cho module khac. */
    @Override
    public Optional<BookingSummary> findByApplicationId(UUID applicationId) {
        return bookingRepository.findByApplicationId(applicationId).map(this::toSummary);
    }

    /**
     * Tao booking-lite moi - UNIQUE(application_id) o V31 tu chan tao trung neu bi goi lai
     * nham cho cung 1 application. Khong goi nguoc lai Task/Chat o day (tranh circular bean
     * dependency da gap va sua o Round B3, xem docs/PROGRESS-CHAT-MODULE.md) - moi thong tin
     * can thiet do TaskApplicationService truyen vao san.
     */
    @Override
    @Transactional
    public BookingSummary createFromApplication(UUID applicationId, UUID taskId, UUID posterId, UUID taskerId,
            long feeBaseAmount, Instant initialScheduledAt) {
        Instant now = clock.instant();
        Booking booking = Booking.createFromApplication(UUID.randomUUID(), applicationId, taskId, posterId,
                taskerId, feeBaseAmount, initialScheduledAt, now);
        bookingRepository.save(booking);
        return toSummary(booking);
    }

    /**
     * Gate doc thuan tuy cho UC16 muc 9 (doi lich) - booking phai ton tai cho application nay
     * va task cua no phai dang ASSIGNED (chua COMPLETED). Khong ghi gi vao DB - xem Javadoc
     * BookingFacade.proposeReschedule().
     */
    @Override
    @Transactional(readOnly = true)
    public void proposeReschedule(UUID applicationId) {
        Booking booking = requireBooking(applicationId);
        TaskStatus taskStatus = taskFacade.findTask(booking.getTaskId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESCHEDULE_NOT_ALLOWED))
                .status();
        if (taskStatus != TaskStatus.ASSIGNED) {
            throw new BusinessException(ErrorCode.RESCHEDULE_NOT_ALLOWED);
        }
    }

    /** Cap nhat scheduled_at cua booking sau khi 1 RESCHEDULE_PROPOSAL duoc Dong y - khong dung den escrow/phi. */
    @Override
    @Transactional
    public BookingSummary acceptReschedule(UUID applicationId, Instant proposedTime) {
        Booking booking = requireBooking(applicationId);
        booking.updateScheduledAt(proposedTime, clock.instant());
        return toSummary(booking);
    }

    /** Booking phai ton tai cho application nay - BOOKING_NOT_FOUND neu UC11 chua tung chon ung vien nay. */
    private Booking requireBooking(UUID applicationId) {
        return bookingRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
    }

    /** Anh xa entity Booking sang DTO cong khai BookingSummary. */
    private BookingSummary toSummary(Booking booking) {
        return new BookingSummary(booking.getId(), booking.getApplicationId(), booking.getTaskId(),
                booking.getPosterId(), booking.getTaskerId(), booking.getFeeBaseAmount(), booking.getStatus(),
                booking.getScheduledAt());
    }
}
