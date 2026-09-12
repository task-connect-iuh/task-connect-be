package vn.taskconnect.user.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.user.entity.SavedAddress;

/**
 * Truy xuat du lieu bang user_saved_addresses. Chi module User duoc inject truc tiep
 * repository nay - module khac phai goi qua UserFacade.
 */
public interface SavedAddressRepository extends JpaRepository<SavedAddress, UUID> {

    /** Toan bo dia chi da luu cua mot tai khoan, moi luu gan day nhat truoc - dung cho danh sach chon nhanh. */
    List<SavedAddress> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
