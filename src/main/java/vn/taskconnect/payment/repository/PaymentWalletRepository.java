package vn.taskconnect.payment.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.payment.entity.PaymentWallet;

/**
 * Truy xuat du lieu bang payment_wallets. Chi module Payment duoc inject truc tiep repository
 * nay - module khac phai goi qua PaymentFacade.
 */
public interface PaymentWalletRepository extends JpaRepository<PaymentWallet, UUID> {

    /** Vi cua 1 tai khoan - UNIQUE(account_id), toi da 1 ket qua. */
    Optional<PaymentWallet> findByAccountId(UUID accountId);
}
