package vn.taskconnect.payment.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.payment.api.PaymentFacade;
import vn.taskconnect.payment.api.dto.EscrowHoldSummary;
import vn.taskconnect.payment.entity.PaymentEscrowHold;
import vn.taskconnect.payment.entity.PaymentWallet;
import vn.taskconnect.payment.entity.PaymentWalletTransaction;
import vn.taskconnect.payment.repository.PaymentEscrowHoldRepository;
import vn.taskconnect.payment.repository.PaymentWalletRepository;
import vn.taskconnect.payment.repository.PaymentWalletTransactionRepository;

/**
 * Trien khai duy nhat cua PaymentFacade - khong module nao khac trong payment duoc implements
 * interface nay. "Nap tien" dot nay la GIA LAP (quyet dinh nguoi dung 2026-10-02): khong goi
 * Sepay hay cong thanh toan nao, chi ghi nhan 1 dong DEPOSIT_SIMULATED vao vi roi chuyen thang
 * vao tam giu trong CUNG 1 transaction - luon thanh cong, khong co nhanh that bai. Moi lan doi
 * balance/heldAmount deu di kem dung 1 dong payment_wallet_transactions (quy tac 2/3 cua
 * .claude/rules/14-payment-escrow.md), khong bao gio UPDATE truc tiep ma khong ghi so.
 */
@Service
class PaymentFacadeImpl implements PaymentFacade {

    private final PaymentWalletRepository walletRepository;
    private final PaymentWalletTransactionRepository transactionRepository;
    private final PaymentEscrowHoldRepository escrowHoldRepository;
    private final Clock clock;

    PaymentFacadeImpl(PaymentWalletRepository walletRepository,
            PaymentWalletTransactionRepository transactionRepository,
            PaymentEscrowHoldRepository escrowHoldRepository, Clock clock) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.escrowHoldRepository = escrowHoldRepository;
        this.clock = clock;
    }

    /**
     * Giu tien lan dau cho 1 booking vua tao - gia lap nap du holdAmount vao vi Poster roi
     * chuyen thang vao tam giu, cuoi cung tao ban ghi payment_escrow_holds. Net balance cua vi
     * tro ve 0 sau cung (nap bao nhieu giu het bay nhieu) - so giao dich van day du truy vet.
     */
    @Override
    @Transactional
    public EscrowHoldSummary holdInitial(UUID bookingId, UUID posterId, UUID taskerId, long holdAmount) {
        PaymentWallet wallet = findOrCreateWallet(posterId);
        depositSimulated(wallet, holdAmount, bookingId, "Nạp tiền giả lập để giữ cho công việc này.");
        moveToInitialHold(wallet, holdAmount, bookingId);
        PaymentEscrowHold hold = PaymentEscrowHold.open(UUID.randomUUID(), bookingId, posterId, taskerId, holdAmount,
                clock.instant());
        escrowHoldRepository.save(hold);
        return toSummary(hold);
    }

    /** Doc ban ghi tam giu theo bookingId, rong neu booking do chua tung duoc giu tien. */
    @Override
    @Transactional(readOnly = true)
    public Optional<EscrowHoldSummary> findEscrowSummary(UUID bookingId) {
        return escrowHoldRepository.findByBookingId(bookingId).map(this::toSummary);
    }

    /**
     * Nap them (gia lap) phan chenh lech de so dang giu dat dung targetHeldAmount - khong lam gi
     * neu targetHeldAmount khong lon hon so hien tai (tranh nap am/nap trung khi goi lai, vd
     * Poster bam "Nap" nhieu lan hoac khong con gi can nap them). Ban ghi tam giu PHAI da ton
     * tai (tao boi holdInitial luc confirm) - thieu la loi bat bien he thong (IllegalStateException,
     * khong phai loi nguoi dung, cung cach AdminFacadeImpl.requireParam xu ly tham so thieu).
     */
    @Override
    @Transactional
    public EscrowHoldSummary topUpToTarget(UUID bookingId, long targetHeldAmount) {
        PaymentEscrowHold hold = escrowHoldRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalStateException(
                        "Booking " + bookingId + " chưa từng được giữ tiền lần đầu, không thể nạp thêm."));
        long delta = targetHeldAmount - hold.getHeldAmount();
        if (delta <= 0) {
            return toSummary(hold);
        }
        PaymentWallet wallet = findOrCreateWallet(hold.getPosterId());
        depositSimulated(wallet, delta, bookingId, "Nạp thêm giả lập cho chi phí phát sinh đã được duyệt.");
        moveToTopUpHold(wallet, delta, bookingId);
        hold.increaseHeldAmount(delta, clock.instant());
        return toSummary(hold);
    }

    /** Lay vi hien co cua 1 tai khoan, hoac tao moi (so du 0) neu day la lan dau can toi vi. */
    private PaymentWallet findOrCreateWallet(UUID accountId) {
        return walletRepository.findByAccountId(accountId)
                .orElseGet(() -> walletRepository.save(PaymentWallet.createEmpty(UUID.randomUUID(), accountId,
                        clock.instant())));
    }

    /** Ghi nhan tien gia lap "nap vao" vi - LUON goi truoc 1 trong 2 ham moveTo*Hold ben duoi, trong cung transaction. */
    private void depositSimulated(PaymentWallet wallet, long amount, UUID bookingId, String note) {
        Instant now = clock.instant();
        wallet.deposit(amount, now);
        transactionRepository.save(PaymentWalletTransaction.depositSimulated(UUID.randomUUID(), wallet.getId(),
                amount, bookingId, note, now));
    }

    /** Chuyen tien tu vi sang tam giu LAN DAU - dung trong holdInitial(). */
    private void moveToInitialHold(PaymentWallet wallet, long amount, UUID bookingId) {
        Instant now = clock.instant();
        wallet.withdraw(amount, now);
        transactionRepository.save(
                PaymentWalletTransaction.escrowHold(UUID.randomUUID(), wallet.getId(), amount, bookingId, now));
    }

    /** Chuyen tien tu vi sang tam giu do NAP THEM - dung trong topUpToTarget(). */
    private void moveToTopUpHold(PaymentWallet wallet, long amount, UUID bookingId) {
        Instant now = clock.instant();
        wallet.withdraw(amount, now);
        transactionRepository.save(
                PaymentWalletTransaction.escrowTopUp(UUID.randomUUID(), wallet.getId(), amount, bookingId, now));
    }

    /** Anh xa entity PaymentEscrowHold sang DTO cong khai EscrowHoldSummary. */
    private EscrowHoldSummary toSummary(PaymentEscrowHold hold) {
        return new EscrowHoldSummary(hold.getId(), hold.getBookingId(), hold.getHeldAmount(), hold.getStatus(),
                hold.getUpdatedAt());
    }
}
