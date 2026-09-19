package vn.taskconnect.user.api.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.api.SkillVerificationStatus;

/**
 * Ho so mot Tasker ung vien cho mot nhom dich vu, dung boi module Matching de loc/tinh diem
 * cau truc (khoang cach, gia, kinh nghiem, lich ranh) khi goi y Tasker cho mot Task - xem
 * {@link vn.taskconnect.user.api.UserFacade#findMatchCandidates(UUID)}. Gop du lieu tu ba
 * bang cua module User (user_profiles, user_tasker_skill_profiles, user_tasker_availability)
 * thanh mot DTO phang de Matching khong phai goi facade nhieu lan cho tung bang.
 *
 * @param accountId id tai khoan Tasker
 * @param categoryId nhom dich vu dang xet (trung voi tham so categoryId da truyen vao)
 * @param verificationStatus trang thai xac minh ho so ky nang cho category nay
 * @param kycStatus trang thai xac minh danh tinh (KYC) cua chinh tai khoan Tasker - KHAC voi
 *                  verificationStatus (do la xac minh ho so ky nang, gan voi tung category).
 *                  Dung de loc "Xem them" (mo rong pham vi tim kiem) chi goi y nguoi da xac
 *                  minh danh tinh, xem TaskerMatchingService.rankCandidates(expand).
 * @param priceMin gia toi thieu Tasker chao cho category nay, null neu khong khai bao
 * @param priceMax gia toi da Tasker chao cho category nay, null neu khong khai bao
 * @param yearsExperience so nam kinh nghiem Tasker tu khai cho category nay
 * @param locationLat vi do hoat dong cua Tasker, null neu chua khai bao toa do
 * @param locationLng kinh do hoat dong cua Tasker, null neu chua khai bao toa do
 * @param preferredRadiusKm ban kinh lam viec uu tien (km) Tasker tu khai, null neu chua khai bao
 * @param bio gioi thieu ban than dang van ban tu do, dung lam ngu lieu embedding ngu nghia
 * @param availability danh sach khung gio ranh trong tuan cua Tasker
 */
public record TaskerMatchCandidateSummary(
        UUID accountId,
        UUID categoryId,
        SkillVerificationStatus verificationStatus,
        KycStatus kycStatus,
        Long priceMin,
        Long priceMax,
        int yearsExperience,
        BigDecimal locationLat,
        BigDecimal locationLng,
        Integer preferredRadiusKm,
        String bio,
        List<AvailabilitySlot> availability
) {

    /** Mot khung gio ranh trong tuan, day = 1 (Thu 2) ... 7 (Chu nhat) - dung khop TaskerAvailability. */
    public record AvailabilitySlot(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }
}
