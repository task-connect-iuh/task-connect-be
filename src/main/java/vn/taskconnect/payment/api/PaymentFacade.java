package vn.taskconnect.payment.api;

import java.util.Optional;
import java.util.UUID;
import vn.taskconnect.payment.api.dto.EscrowHoldSummary;

/**
 * Be mat cong khai duy nhat cua module Payment. Module khac chi duoc goi qua day, cam import
 * entity trong {@code payment.entity} hoac inject repository cua module Payment. Dot nay
 * ("escrow-lite", quyet dinh nguoi dung 2026-10-02) chi du de UC11 giu tien that (gia lap, chua
 * Sepay) - khong co giai ngan/hoan tien, xem Javadoc EscrowHoldStatus. Chi duoc goi tu module
 * Booking (Booking -> Payment, dong bo, da cho phep san trong 10-module-boundary.md) - khong
 * module nao khac duoc inject truc tiep PaymentFacade.
 */
public interface PaymentFacade {

    /**
     * Giu tien lan dau cho 1 booking vua tao - gia lap nap du holdAmount vao vi cua posterId
     * roi chuyen thang vao tam giu (2 dong so giao dich trong CUNG 1 transaction, xem
     * PaymentFacadeImpl). Vi duoc tao lazy neu posterId chua tung co vi. Chi duoc goi dung 1 lan
     * cho moi bookingId (UNIQUE(booking_id), xem V48) - goi lai se vi pham constraint.
     */
    EscrowHoldSummary holdInitial(UUID bookingId, UUID posterId, UUID taskerId, long holdAmount);

    /** Doc ban ghi tam giu theo bookingId, rong neu booking do chua tung duoc giu tien. */
    Optional<EscrowHoldSummary> findEscrowSummary(UUID bookingId);

    /**
     * Nap them (gia lap) de tam giu dat dung targetHeldAmount - dung khi Poster bam "Nap" sau
     * khi mot batch chi phi phat sinh duoc duyet (so tien phai tra tang len). Khong lam gi (tra
     * ve nguyen trang) neu targetHeldAmount <= so dang giu hien tai - tranh nap am/nap trung khi
     * goi lai.
     */
    EscrowHoldSummary topUpToTarget(UUID bookingId, long targetHeldAmount);
}
