package vn.taskconnect.booking.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import vn.taskconnect.booking.api.dto.BookingEscrowSummary;
import vn.taskconnect.booking.api.dto.BookingSummary;

/**
 * Be mat cong khai duy nhat cua module Booking. Module khac chi duoc goi qua day, cam import
 * entity trong {@code booking.entity} hoac inject repository cua module Booking. Dot nay
 * ("booking-lite") chi du de UC11 co 1 booking row that - xem
 * TaskConnect_Chat_ImplementationSpec.md va docs/PROGRESS-CHAT-MODULE.md muc 0.2.
 * proposeReschedule()/acceptReschedule() them tu Round B6 (doi lich lam viec, UC16 muc 9).
 * getEscrowBreakdown()/topUpEscrow()/findByTaskId() them 2026-10-02 (UC11 chon phuong thuc thanh
 * toan + tinh nang chi phi phat sinh, quyet dinh nguoi dung) - Booking la noi duy nhat ghep
 * feeBaseAmount/paymentMethod cua chinh no voi heldAmount that cua Payment (goi qua
 * Booking -> Payment, dong bo), de Task khong can biet gi ve module Payment.
 */
public interface BookingFacade {

    /** Booking gan voi 1 application, rong neu application do chua duoc chon o UC11. */
    Optional<BookingSummary> findByApplicationId(UUID applicationId);

    /** Booking gan voi 1 task, rong neu task do chua tung duoc assign (UC11) - dung de Task tra winningApplicationId cho FE. */
    Optional<BookingSummary> findByTaskId(UUID taskId);

    /**
     * Tao 1 booking-lite ngay sau khi Poster chon xong 1 ung vien o UC11 VA giu tien thanh cong
     * qua PaymentFacade (xem BookingFacadeImpl) - luon CONFIRMED (GUARDRAIL 2 CLAUDE.md da thoa
     * man that su tu 2026-10-02, khac PENDING_ESCROW truoc day khi Payment chua ton tai). Chi
     * duoc goi tu TaskApplicationService.confirm(), khong co endpoint rieng nao khac tao booking.
     */
    BookingSummary createFromApplication(UUID applicationId, UUID taskId, UUID posterId, UUID taskerId,
            long feeBaseAmount, PaymentMethod paymentMethod, Instant initialScheduledAt);

    /**
     * Lat cat day du tien cua 1 application de TaskExtraCostService gate/hien thi: feeBaseAmount
     * + paymentMethod cua Booking, ghep voi heldAmount/status that tu Payment. Rong neu
     * application do chua tung co booking (chua tung duoc chon o UC11).
     */
    Optional<BookingEscrowSummary> getEscrowBreakdown(UUID applicationId);

    /**
     * Nap them (gia lap) de tam giu cua booking gan voi application nay dat dung targetHeldAmount
     * - dung khi Poster bam "Nap" sau khi 1 batch chi phi phat sinh duoc duyet. Nem
     * BOOKING_NOT_FOUND neu application chua tung co booking.
     */
    BookingEscrowSummary topUpEscrow(UUID applicationId, long targetHeldAmount);

    /**
     * Xac nhan co the de xuat doi lich cho booking cua application nay - CHI khi task dang
     * ASSIGNED (dac ta muc 9, Round B6). Khong ghi gi vao DB (gate thuan tuy doc) - gia tri de
     * xuat that su chi ton tai trong chat_messages.proposed_time cho toi khi duoc Dong y, xem
     * ChatService.createRescheduleProposal(). Nem BOOKING_NOT_FOUND neu application chua tung
     * duoc chon o UC11 (chua co booking), RESCHEDULE_NOT_ALLOWED neu task khong con ASSIGNED.
     */
    void proposeReschedule(UUID applicationId);

    /**
     * Ghi nhan 1 de xuat doi lich duoc Dong y - cap nhat booking_bookings.scheduled_at VA dong
     * bo sang task_tasks.scheduled_at qua TaskFacade.syncScheduledAt() (moi noi FE hien "gio dang
     * hen" deu doc tu Task), KHONG dung den escrow/phi (dac ta muc 9). Tra ve booking da cap nhat
     * de Chat dung trong SYSTEM message.
     */
    BookingSummary acceptReschedule(UUID applicationId, Instant proposedTime);
}
