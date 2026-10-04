package vn.taskconnect.payment.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.payment.entity.PaymentWalletTransaction;

/**
 * Truy xuat du lieu bang payment_wallet_transactions (append-only). Chi module Payment duoc
 * inject truc tiep repository nay - module khac phai goi qua PaymentFacade.
 */
public interface PaymentWalletTransactionRepository extends JpaRepository<PaymentWalletTransaction, UUID> {
}
