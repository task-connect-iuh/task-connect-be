package vn.taskconnect.booking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.admin.api.AdminFacade;
import vn.taskconnect.booking.api.BookingFacade;
import vn.taskconnect.booking.api.PaymentMethod;
import vn.taskconnect.booking.api.dto.BookingEscrowSummary;
import vn.taskconnect.booking.api.dto.BookingSummary;
import vn.taskconnect.booking.entity.Booking;
import vn.taskconnect.booking.repository.BookingRepository;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.payment.api.PaymentFacade;
import vn.taskconnect.payment.api.dto.EscrowHoldSummary;
import vn.taskconnect.task.api.TaskFacade;
import vn.taskconnect.task.api.TaskStatus;

/**
 * Trien khai duy nhat cua BookingFacade - khong module nao khac trong booking duoc implements
 * interface nay. Tu Round B6 phu thuoc them TaskFacade (Booking -> Task, da cho phep san trong
 * 10-module-boundary.md) de kiem tra task.status khi xet de xuat doi lich - AN TOAN ve vong
 * tron bean vi TaskFacadeImpl khong phu thuoc nguoc lai BookingFacade (xem quy tac phat hien o
 * Round B3, docs/PROGRESS-CHAT-MODULE.md). Tu 2026-10-02 phu thuoc them PaymentFacade
 * (Booking -> Payment, dong bo, da cho phep san) de giu tien that luc tao booking, va AdminFacade
 * (doc ty le phi nen tang de tinh holdAmount cho PA FEE_ONLY_ESCROW).
 */
@Service
class BookingFacadeImpl implements BookingFacade {

    private final BookingRepository bookingRepository;
    private final TaskFacade taskFacade;
    private final PaymentFacade paymentFacade;
    private final AdminFacade adminFacade;
    private final Clock clock;

    BookingFacadeImpl(BookingRepository bookingRepository, TaskFacade taskFacade, PaymentFacade paymentFacade,
            AdminFacade adminFacade, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.taskFacade = taskFacade;
        this.paymentFacade = paymentFacade;
        this.adminFacade = adminFacade;
        this.clock = clock;
    }

    /** Doc booking theo applicationId va anh xa sang BookingSummary de tra cho module khac. */
    @Override
    public Optional<BookingSummary> findByApplicationId(UUID applicationId) {
        return bookingRepository.findByApplicationId(applicationId).map(this::toSummary);
    }

    /** Doc booking theo taskId va anh xa sang BookingSummary - dung de Task tra winningApplicationId cho FE. */
    @Override
    public Optional<BookingSummary> findByTaskId(UUID taskId) {
        return bookingRepository.findByTaskId(taskId).map(this::toSummary);
    }

    /**
     * Tao booking-lite moi - UNIQUE(application_id) o V31 tu chan tao trung neu bi goi lai
     * nham cho cung 1 application. Khong goi nguoc lai Task/Chat o day (tranh circular bean
     * dependency da gap va sua o Round B3, xem docs/PROGRESS-CHAT-MODULE.md) - moi thong tin
     * can thiet do TaskApplicationService truyen vao san. Tu 2026-10-02: tinh holdAmount theo
     * paymentMethod (FULL_ESCROW giu nguyen feeBaseAmount, FEE_ONLY_ESCROW chi giu 8% phi nen
     * tang - lam tron XUONG, cung quy tac voi TaskApplicationService.computePlatformFee) roi goi
     * PaymentFacade.holdInitial() giu tien that - neu buoc giu tien loi, @Transactional rollback
     * ca Booking (GUARDRAIL 2 CLAUDE.md). Booking duoc save() (enqueue insert) TRUOC khi goi
     * holdInitial() - PaymentEscrowHold.bookingId la cot UUID thuong, KHONG phai quan he JPA
     * (@ManyToOne) toi Booking, nen Hibernate khong biet thu tu FK phu thuoc va flush insert theo
     * dung thu tu duoc enqueue; neu holdInitial() enqueue truoc, flush se insert payment_escrow_holds
     * truoc booking_bookings va vi pham FK (da gap khi auto-flush boi 1 SELECT sau do trong cung
     * transaction, xem BookingFacadeImpl.getEscrowBreakdown()).
     */
    @Override
    @Transactional
    public BookingSummary createFromApplication(UUID applicationId, UUID taskId, UUID posterId, UUID taskerId,
            long feeBaseAmount, PaymentMethod paymentMethod, Instant initialScheduledAt) {
        Instant now = clock.instant();
        UUID bookingId = UUID.randomUUID();
        long holdAmount = computeHoldAmount(feeBaseAmount, paymentMethod);
        Booking booking = Booking.createFromApplication(bookingId, applicationId, taskId, posterId, taskerId,
                feeBaseAmount, paymentMethod, initialScheduledAt, now);
        bookingRepository.save(booking);
        paymentFacade.holdInitial(bookingId, posterId, taskerId, holdAmount);
        return toSummary(booking);
    }

    /**
     * Ghep feeBaseAmount/paymentMethod cua Booking voi heldAmount/status that cua Payment - rong
     * neu application chua tung co booking. Khong nen xay ra truong hop co booking ma khong co
     * escrow hold (holdInitial() luon goi truoc khi Booking duoc luu, xem createFromApplication)
     * nen escrow hold thieu la loi bat bien he thong (IllegalStateException).
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<BookingEscrowSummary> getEscrowBreakdown(UUID applicationId) {
        return bookingRepository.findByApplicationId(applicationId).map(booking -> {
            EscrowHoldSummary hold = paymentFacade.findEscrowSummary(booking.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Booking " + booking.getId() + " chưa từng được giữ tiền."));
            return new BookingEscrowSummary(booking.getId(), booking.getFeeBaseAmount(), booking.getPaymentMethod(),
                    hold.heldAmount(), hold.status());
        });
    }

    /** Nap them (gia lap) de tam giu cua booking gan voi application nay dat dung targetHeldAmount - uy quyen cho PaymentFacade. */
    @Override
    @Transactional
    public BookingEscrowSummary topUpEscrow(UUID applicationId, long targetHeldAmount) {
        Booking booking = requireBooking(applicationId);
        EscrowHoldSummary hold = paymentFacade.topUpToTarget(booking.getId(), targetHeldAmount);
        return new BookingEscrowSummary(booking.getId(), booking.getFeeBaseAmount(), booking.getPaymentMethod(),
                hold.heldAmount(), hold.status());
    }

    /**
     * So tien phai giu that luc tao booking, theo phuong thuc thanh toan da chon (quyet dinh
     * nguoi dung 2026-10-02): FULL_ESCROW giu nguyen fee_base; FEE_ONLY_ESCROW chi giu phi nen
     * tang (fee_base x ty le, lam tron XUONG ve dong nguyen - cung quy tac lam tron voi
     * TaskApplicationService.computePlatformFee(), phan du thuoc ve Tasker).
     */
    private long computeHoldAmount(long feeBaseAmount, PaymentMethod paymentMethod) {
        if (paymentMethod == PaymentMethod.FULL_ESCROW) {
            return feeBaseAmount;
        }
        BigDecimal feeRate = adminFacade.getPlatformFeeRate();
        return BigDecimal.valueOf(feeBaseAmount).multiply(feeRate).setScale(0, RoundingMode.DOWN).longValueExact();
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

    /**
     * Cap nhat scheduled_at cua booking sau khi 1 RESCHEDULE_PROPOSAL duoc Dong y - khong dung
     * den escrow/phi. Dong bo luon sang Task.scheduledAt qua TaskFacade (bao cao nguoi dung
     * 2026-09-30: FE moi cho hien "gio dang hen" deu doc tu Task, khong biet gi ve Booking - thieu
     * buoc nay se lam moi noi hien thi ngoai chinh RESCHEDULE_PROPOSAL bi ket qua gio CU sau khi
     * da Dong y doi lich).
     */
    @Override
    @Transactional
    public BookingSummary acceptReschedule(UUID applicationId, Instant proposedTime) {
        Booking booking = requireBooking(applicationId);
        booking.updateScheduledAt(proposedTime, clock.instant());
        taskFacade.syncScheduledAt(booking.getTaskId(), proposedTime);
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
                booking.getPosterId(), booking.getTaskerId(), booking.getFeeBaseAmount(),
                booking.getPaymentMethod(), booking.getStatus(), booking.getScheduledAt());
    }
}
