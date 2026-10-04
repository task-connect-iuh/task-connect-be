package vn.taskconnect.payment.entity;

/**
 * Loai 1 dong payment_wallet_transactions - chi tiet trien khai noi bo cua PaymentWalletTransaction
 * (khong public, khong lo ra ngoai module Payment qua api/). DEPOSIT_SIMULATED la tien gia lap
 * "nap vao" vi (tang balance); ESCROW_HOLD/ESCROW_TOPUP la tien "chuyen di" sang tam giu (giam
 * balance) - tach rieng 2 loai nay chi de doc lai so giao dich de phan biet duoc lan giu dau
 * (holdInitial) voi lan nap them sau do (topUpToTarget), xem PaymentFacadeImpl.
 */
enum WalletTransactionType {
    DEPOSIT_SIMULATED,
    ESCROW_HOLD,
    ESCROW_TOPUP
}
