package vn.taskconnect.payment.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.payment.entity.PaymentEscrowHold;

/**
 * Truy xuat du lieu bang payment_escrow_holds. Chi module Payment duoc inject truc tiep
 * repository nay - module khac phai goi qua PaymentFacade.
 */
public interface PaymentEscrowHoldRepository extends JpaRepository<PaymentEscrowHold, UUID> {

    /** Ban ghi tam giu cua 1 booking - UNIQUE(booking_id), toi da 1 ket qua. */
    Optional<PaymentEscrowHold> findByBookingId(UUID bookingId);
}
