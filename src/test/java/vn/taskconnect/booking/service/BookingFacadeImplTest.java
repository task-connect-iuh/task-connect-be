package vn.taskconnect.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.taskconnect.admin.api.AdminFacade;
import vn.taskconnect.booking.api.BookingStatus;
import vn.taskconnect.booking.api.PaymentMethod;
import vn.taskconnect.booking.api.dto.BookingSummary;
import vn.taskconnect.booking.entity.Booking;
import vn.taskconnect.booking.repository.BookingRepository;
import vn.taskconnect.payment.api.EscrowHoldStatus;
import vn.taskconnect.payment.api.PaymentFacade;
import vn.taskconnect.payment.api.dto.EscrowHoldSummary;
import vn.taskconnect.task.api.TaskFacade;

/**
 * Unit test thuan tuy cho BookingFacadeImpl.createFromApplication() - trong tam vao
 * computeHoldAmount() (NGUON SU THAT DUY NHAT cho so tien giu theo tung PaymentMethod, xem
 * docs/PROGRESS-PAYMENT-MODULE.md) vi day la logic tien bac de sai nhat trong dot 2026-10-02.
 */
class BookingFacadeImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-02T10:00:00Z");
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();
    private static final UUID POSTER_ID = UUID.randomUUID();
    private static final UUID TASKER_ID = UUID.randomUUID();

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final TaskFacade taskFacade = mock(TaskFacade.class);
    private final PaymentFacade paymentFacade = mock(PaymentFacade.class);
    private final AdminFacade adminFacade = mock(AdminFacade.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final BookingFacadeImpl facade = new BookingFacadeImpl(
            bookingRepository, taskFacade, paymentFacade, adminFacade, clock);

    private void stubHoldInitial() {
        when(paymentFacade.holdInitial(any(), eq(POSTER_ID), eq(TASKER_ID), anyLong()))
                .thenAnswer(invocation -> new EscrowHoldSummary(UUID.randomUUID(), invocation.getArgument(0),
                        invocation.getArgument(3), EscrowHoldStatus.HELD, FIXED_NOW));
    }

    @Test
    void should_holdFullFeeBase_when_paymentMethodIsFullEscrow() {
        stubHoldInitial();
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingSummary summary = facade.createFromApplication(APPLICATION_ID, TASK_ID, POSTER_ID, TASKER_ID,
                500_000L, PaymentMethod.FULL_ESCROW, FIXED_NOW);

        verify(paymentFacade).holdInitial(any(), eq(POSTER_ID), eq(TASKER_ID), eq(500_000L));
        assertThat(summary.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(summary.paymentMethod()).isEqualTo(PaymentMethod.FULL_ESCROW);
        assertThat(summary.feeBaseAmount()).isEqualTo(500_000L);
    }

    @Test
    void should_holdOnlyPlatformFeeRoundedDown_when_paymentMethodIsFeeOnlyEscrow() {
        stubHoldInitial();
        when(adminFacade.getPlatformFeeRate()).thenReturn(new BigDecimal("0.08"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 333.333 x 8% = 26.666,64 -> phai lam tron XUONG ve 26.666 (khong lam tron len/gan nhat).
        BookingSummary summary = facade.createFromApplication(APPLICATION_ID, TASK_ID, POSTER_ID, TASKER_ID,
                333_333L, PaymentMethod.FEE_ONLY_ESCROW, FIXED_NOW);

        verify(paymentFacade).holdInitial(any(), eq(POSTER_ID), eq(TASKER_ID), eq(26_666L));
        assertThat(summary.paymentMethod()).isEqualTo(PaymentMethod.FEE_ONLY_ESCROW);
        assertThat(summary.feeBaseAmount()).isEqualTo(333_333L);
    }
}
