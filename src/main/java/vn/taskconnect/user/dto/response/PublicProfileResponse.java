package vn.taskconnect.user.dto.response;

import java.util.List;
import java.util.UUID;
import vn.taskconnect.auth.api.dto.AccountSummary;
import vn.taskconnect.user.entity.UserProfile;

/**
 * Ho so toi thieu, khong nhay cam, dung khi mot tai khoan xem ho so cua nguoi khac (vi du
 * Task/Review sau nay hien thi ten va khu vuc hoat dong cua Tasker). Khong lo addressText
 * chi tiet hay toa do chinh xac. bio duoc lo cong khai (khac addressText) vi la doan gioi
 * thieu ban than nguoi dung chu dong viet de nguoi khac doc, khong phai thong tin dinh vi.
 * verifiedSkills lo them cac nhom dich vu da VERIFIED de FE hien badge "Da xac minh" -
 * khong lo ho so PENDING/REJECTED, khong lo kinh nghiem/gia/chung chi. availability lo lich
 * ranh trong tuan (thu + khung gio) de Poster xem truoc Tasker ranh luc nao - khong nhay
 * cam, chi la thong tin tu khai khong qua duyet, giong het du lieu Tasker tu xem o
 * GET /users/me/tasker-availability. email/phone doc tu AuthAccount qua AuthFacade - lo ra
 * theo yeu cau nghiep vu (nguoi xem ho so can lien he truc tiep), null neu tai khoan khong
 * co (vd phone chua khai bao).
 */
public record PublicProfileResponse(
        UUID accountId,
        String fullName,
        String avatarUrl,
        String bio,
        String operatingArea,
        List<PublicVerifiedSkillResponse> verifiedSkills,
        List<AvailabilitySlotResponse> availability,
        String email,
        String phone
) {

    /** Chuyen entity sang DTO cong khai, chi lay cac truong khong nhay cam, kem danh sach ky nang da xac minh va lich ranh do service lap rap san. */
    public static PublicProfileResponse from(UserProfile profile, List<PublicVerifiedSkillResponse> verifiedSkills,
            List<AvailabilitySlotResponse> availability, AccountSummary account) {
        return new PublicProfileResponse(
                profile.getAccountId(),
                profile.getFullName(),
                profile.getAvatarUrl(),
                profile.getBio(),
                profile.getOperatingArea(),
                verifiedSkills,
                availability,
                account != null ? account.email() : null,
                account != null ? account.phone() : null);
    }
}
