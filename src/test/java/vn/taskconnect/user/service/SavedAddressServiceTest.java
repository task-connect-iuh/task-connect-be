package vn.taskconnect.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.user.dto.request.CreateSavedAddressRequest;
import vn.taskconnect.user.entity.SavedAddress;
import vn.taskconnect.user.repository.SavedAddressRepository;

/**
 * Unit test thuan tuy (khong DB) cho SavedAddressService, tap trung vao gioi han
 * MAX_SAVED_ADDRESSES_PER_ACCOUNT (chot cung nguoi dung 2026-09-14).
 */
class SavedAddressServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final Instant FIXED_NOW = Instant.parse("2026-09-14T10:00:00Z");

    private final SavedAddressRepository repository = mock(SavedAddressRepository.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final SavedAddressService service = new SavedAddressService(repository, clock);

    private CreateSavedAddressRequest sampleRequest() {
        return new CreateSavedAddressRequest("Nha", "123 Le Loi", BigDecimal.valueOf(10.77),
                BigDecimal.valueOf(106.70), null, null);
    }

    @Test
    void should_saveAddress_when_underLimit() {
        when(repository.countByAccountId(ACCOUNT_ID)).thenReturn(4L);
        when(repository.save(any(SavedAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SavedAddress result = service.addAddress(ACCOUNT_ID, sampleRequest());

        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getLabel()).isEqualTo("Nha");
    }

    @Test
    void should_throwLimitReached_when_accountAlreadyHasFiveAddresses() {
        when(repository.countByAccountId(ACCOUNT_ID)).thenReturn(5L);

        assertThatThrownBy(() -> service.addAddress(ACCOUNT_ID, sampleRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.SAVED_ADDRESS_LIMIT_REACHED);
        Mockito.verify(repository, never()).save(any());
    }
}
