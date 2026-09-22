package vn.taskconnect.user.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.taskconnect.user.api.dto.ServiceCategorySummary;
import vn.taskconnect.user.api.dto.TaskerMatchCandidateSummary;
import vn.taskconnect.user.api.dto.UserProfileSummary;

/**
 * Be mat cong khai duy nhat cua module User. Module khac chi duoc goi qua day, cam import
 * entity trong {@code user.entity} hoac inject repository cua module User.
 */
public interface UserFacade {

    /**
     * Doc thong tin toi thieu ho so ca nhan theo accountId. Rong neu tai khoan chua tung
     * tao ho so (xem quyet dinh lazy-create trong docs/PROGRESS-USER-MODULE.md).
     */
    Optional<UserProfileSummary> findProfile(UUID accountId);

    /**
     * Danh sach danh muc nhom dich vu con hien hanh, sap theo ten. Dung khi Task can hien
     * danh sach chon luc dang cong viec, hoac Matching can loc theo nhom.
     */
    List<ServiceCategorySummary> listActiveServiceCategories();

    /**
     * Tao khung ho so ban dau ngay khi tai khoan duoc dang ky, voi ho ten nguoi dung da
     * nhap o form dang ky. operatingArea de rong - nguoi dung tu dien khi vao trang Ho so
     * (khong thu thap duoc o form dang ky). Idempotent: bo qua neu tai khoan da co ho so
     * (khong ghi de len du lieu da ton tai).
     */
    void createInitialProfile(UUID accountId, String fullName);

    /**
     * Tasker co dang cho phep nhan loi moi truc tiep (INVITED, UC09) cho category nay khong.
     * Tra ve false neu Tasker chua tung khai bao ho so ky nang cho category nay (khong co
     * profile de moi vao). Dung boi Task khi Poster moi truc tiep 1 Tasker.
     */
    boolean acceptsDirectInvites(UUID accountId, UUID categoryId);

    /**
     * Xoa hang loat ho so ca nhan theo danh sach accountId. Dung boi Auth
     * (AuthAccountCleanupService) de don du lieu phu thuoc truoc khi xoa chinh tai khoan
     * UNVERIFIED qua han - tranh loi FK RESTRICT tu fk_user_profiles_account (khong khai
     * bao ON DELETE CASCADE). Khong lam gi neu danh sach rong.
     *
     * @return so ho so da xoa, dung de ghi log
     */
    int deleteProfilesByAccountIds(Collection<UUID> accountIds);

    /**
     * Danh sach Tasker co ho so ky nang cho dung mot category, dung boi module Matching
     * (goi y Tasker) de loc/tinh diem cau truc - xem TaskerMatchCandidateSummary. Tra ve MOI
     * trang thai xac minh (khong tu loc VERIFIED o day) de Matching tu quyet dinh dieu kien
     * loc cua minh, dong nhat voi nguyen tac facade chi anh xa du lieu, khong ap logic
     * nghiep vu cua module goi.
     */
    List<TaskerMatchCandidateSummary> findMatchCandidates(UUID categoryId);
}
