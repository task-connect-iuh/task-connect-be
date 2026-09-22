package vn.taskconnect.booking.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import vn.taskconnect.booking.api.dto.BookingSummary;

/**
 * Be mat cong khai duy nhat cua module Booking. Module khac chi duoc goi qua day, cam import
 * entity trong {@code booking.entity} hoac inject repository cua module Booking. Dot nay
 * ("booking-lite") chi du de UC11 co 1 booking row that - xem
 * TaskConnect_Chat_ImplementationSpec.md va docs/PROGRESS-CHAT-MODULE.md muc 0.2.
 * proposeReschedule()/acceptReschedule() them tu Round B6 (doi lich lam viec, UC16 muc 9).
 */
public interface BookingFacade {

    /** Booking gan voi 1 application, rong neu application do chua duoc chon o UC11. */
    Optional<BookingSummary> findByApplicationId(UUID applicationId);

    /**
     * Tao 1 booking-lite ngay sau khi Poster chon xong 1 ung vien o UC11 - luon PENDING_ESCROW
     * (xem Javadoc BookingStatus), khong bao gio CONFIRMED (GUARDRAIL 2 CLAUDE.md). Chi duoc
     * goi tu TaskApplicationService.confirm(), khong co endpoint rieng nao khac tao booking.
     */
    BookingSummary createFromApplication(UUID applicationId, UUID taskId, UUID posterId, UUID taskerId,
            long feeBaseAmount, Instant initialScheduledAt);

    /**
     * Xac nhan co the de xuat doi lich cho booking cua application nay - CHI khi task dang
     * ASSIGNED (dac ta muc 9, Round B6). Khong ghi gi vao DB (gate thuan tuy doc) - gia tri de
     * xuat that su chi ton tai trong chat_messages.proposed_time cho toi khi duoc Dong y, xem
     * ChatService.createRescheduleProposal(). Nem BOOKING_NOT_FOUND neu application chua tung
     * duoc chon o UC11 (chua co booking), RESCHEDULE_NOT_ALLOWED neu task khong con ASSIGNED.
     */
    void proposeReschedule(UUID applicationId);

    /**
     * Ghi nhan 1 de xuat doi lich duoc Dong y - cap nhat booking_bookings.scheduled_at, KHONG
     * dung den escrow/phi (dac ta muc 9). Tra ve booking da cap nhat de Chat dung trong SYSTEM
     * message.
     */
    BookingSummary acceptReschedule(UUID applicationId, Instant proposedTime);
}
