package vn.taskconnect.user.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.user.api.LocationType;
import vn.taskconnect.user.dto.request.CreateSavedAddressRequest;
import vn.taskconnect.user.dto.request.UpdateSavedAddressRequest;
import vn.taskconnect.user.entity.SavedAddress;
import vn.taskconnect.user.repository.SavedAddressRepository;

/**
 * Nghiep vu so dia chi tu luu (chon nhanh khi dang viec) - them, xem, sua, xoa. Doc lap
 * hoan toan voi cac buoc con lai cua module User, chi la thong tin tu khai, khong qua duyet.
 */
@Service
public class SavedAddressService {

    private final SavedAddressRepository addressRepository;
    private final Clock clock;

    public SavedAddressService(SavedAddressRepository addressRepository, Clock clock) {
        this.addressRepository = addressRepository;
        this.clock = clock;
    }

    /** Luu mot dia chi moi. */
    @Transactional
    public SavedAddress addAddress(UUID accountId, CreateSavedAddressRequest request) {
        Instant now = clock.instant();
        SavedAddress address = new SavedAddress(UUID.randomUUID(), accountId, request.label(),
                request.addressText(), request.lat(), request.lng(), request.locationType(),
                request.arrivalNotes(), now);
        return addressRepository.save(address);
    }

    /** Toan bo dia chi da luu cua chinh chu tai khoan, moi luu gan day nhat truoc. */
    @Transactional(readOnly = true)
    public List<SavedAddress> getMyAddresses(UUID accountId) {
        return addressRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    /**
     * Sua mot phan dia chi da luu - truong nao null trong request giu nguyen gia tri cu,
     * cung ngu nghia PATCH da dung o UserProfileService.applyPartialUpdate.
     */
    @Transactional
    public SavedAddress updateAddress(UUID accountId, UUID addressId, UpdateSavedAddressRequest request) {
        SavedAddress address = requireOwnAddress(accountId, addressId);
        String label = request.label() != null ? request.label() : address.getLabel();
        String addressText = request.addressText() != null ? request.addressText() : address.getAddressText();
        BigDecimal lat = request.lat() != null ? request.lat() : address.getLat();
        BigDecimal lng = request.lng() != null ? request.lng() : address.getLng();
        LocationType locationType = request.locationType() != null ? request.locationType() : address.getLocationType();
        String arrivalNotes = request.arrivalNotes() != null ? request.arrivalNotes() : address.getArrivalNotes();
        address.update(label, addressText, lat, lng, locationType, arrivalNotes, clock.instant());
        return addressRepository.save(address);
    }

    /** Xoa mot dia chi da luu. */
    @Transactional
    public void deleteAddress(UUID accountId, UUID addressId) {
        SavedAddress address = requireOwnAddress(accountId, addressId);
        addressRepository.delete(address);
    }

    /**
     * Tim dia chi theo id va bat buoc thuoc ve chinh tai khoan dang goi - khong phan biet
     * "khong ton tai" voi "cua tai khoan khac" trong thong bao loi, tranh lo thong tin qua
     * ma loi (cung nguyen tac kiem tra quyen so huu cua 16-api-contract.md).
     */
    private SavedAddress requireOwnAddress(UUID accountId, UUID addressId) {
        SavedAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVED_ADDRESS_NOT_FOUND));
        if (!address.getAccountId().equals(accountId)) {
            throw new BusinessException(ErrorCode.SAVED_ADDRESS_NOT_FOUND);
        }
        return address;
    }
}
