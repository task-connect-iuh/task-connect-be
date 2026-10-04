package vn.taskconnect.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.taskconnect.payment.api.EscrowHoldStatus;
import vn.taskconnect.payment.api.dto.EscrowHoldSummary;
import vn.taskconnect.payment.entity.PaymentEscrowHold;
import vn.taskconnect.payment.entity.PaymentWallet;
import vn.taskconnect.payment.repository.PaymentEscrowHoldRepository;
import vn.taskconnect.payment.repository.PaymentWalletRepository;
import vn.taskconnect.payment.repository.PaymentWalletTransactionRepository;

/**
 * Unit test thuan tuy (khong DB, khong Spring context) cho PaymentFacadeImpl - trong tam vao
 * holdInitial()/topUpToTarget() vi day la logic tien bac, rui ro cao nhat cua dot "escrow-lite"
 * 2026-10-02 (xem docs/PROGRESS-PAYMENT-MODULE.md).
 */
class PaymentFacadeImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-02T10:00:00Z");
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final UUID POSTER_ID = UUID.randomUUID();
    private static final UUID TASKER_ID = UUID.randomUUID();

    private final PaymentWalletRepository walletRepository = mock(PaymentWalletRepository.class);
    private final PaymentWalletTransactionRepository transactionRepository = mock(PaymentWalletTransactionRepository.class);
    private final PaymentEscrowHoldRepository escrowHoldRepository = mock(PaymentEscrowHoldRepository.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final PaymentFacadeImpl facade = new PaymentFacadeImpl(
            walletRepository, transactionRepository, escrowHoldRepository, clock);

    @Test
    void should_createWalletAndHoldFullAmount_when_posterHasNoWalletYet() {
        when(walletRepository.findByAccountId(POSTER_ID)).thenReturn(Optional.empty());
        when(walletRepository.save(any(PaymentWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowHoldRepository.save(any(PaymentEscrowHold.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EscrowHoldSummary summary = facade.holdInitial(BOOKING_ID, POSTER_ID, TASKER_ID, 500_000L);

        assertThat(summary.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(summary.heldAmount()).isEqualTo(500_000L);
        assertThat(summary.status()).isEqualTo(EscrowHoldStatus.HELD);
        // 2 dong so giao dich: 1 DEPOSIT_SIMULATED (+) va 1 ESCROW_HOLD (-), dung quy tac 2/3 cua 14-payment-escrow.md.
        verify(transactionRepository, times(2)).save(any());
        verify(walletRepository, times(1)).save(any(PaymentWallet.class));
    }

    @Test
    void should_reuseExistingWallet_when_posterAlreadyHasOne() {
        PaymentWallet existingWallet = PaymentWallet.createEmpty(UUID.randomUUID(), POSTER_ID, FIXED_NOW);
        when(walletRepository.findByAccountId(POSTER_ID)).thenReturn(Optional.of(existingWallet));
        when(escrowHoldRepository.save(any(PaymentEscrowHold.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facade.holdInitial(BOOKING_ID, POSTER_ID, TASKER_ID, 200_000L);

        verify(walletRepository, never()).save(any(PaymentWallet.class));
        assertThat(existingWallet.getBalance()).isZero();
    }

    @Test
    void should_notTopUp_when_targetNotGreaterThanCurrentHeld() {
        PaymentEscrowHold hold = PaymentEscrowHold.open(UUID.randomUUID(), BOOKING_ID, POSTER_ID, TASKER_ID,
                500_000L, FIXED_NOW);
        when(escrowHoldRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(hold));

        EscrowHoldSummary summary = facade.topUpToTarget(BOOKING_ID, 500_000L);

        assertThat(summary.heldAmount()).isEqualTo(500_000L);
        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).findByAccountId(any());
    }

    @Test
    void should_topUpDeltaOnly_when_targetGreaterThanCurrentHeld() {
        PaymentEscrowHold hold = PaymentEscrowHold.open(UUID.randomUUID(), BOOKING_ID, POSTER_ID, TASKER_ID,
                500_000L, FIXED_NOW);
        when(escrowHoldRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(hold));
        PaymentWallet existingWallet = PaymentWallet.createEmpty(UUID.randomUUID(), POSTER_ID, FIXED_NOW);
        when(walletRepository.findByAccountId(POSTER_ID)).thenReturn(Optional.of(existingWallet));

        EscrowHoldSummary summary = facade.topUpToTarget(BOOKING_ID, 720_000L);

        assertThat(summary.heldAmount()).isEqualTo(720_000L);
        // Chi nap dung phan chenh lech (220.000), khong nap lai tu dau.
        verify(transactionRepository, times(2)).save(any());
    }
}
