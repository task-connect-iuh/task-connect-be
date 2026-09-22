package vn.taskconnect.admin.api;

import java.math.BigDecimal;

/**
 * Be mat cong khai duy nhat cua module Admin. Module khac chi duoc goi qua day, cam import
 * entity trong {@code admin.entity} hoac inject repository cua module Admin. Dot nay chi lam
 * phan doc nguong van hanh (admin_system_parameters) can cho Chat/Task/Booking - cac phan con
 * lai cua module Admin (duyet ho so, khieu nai, dashboard) chua lam.
 */
public interface AdminFacade {

    /** Ty le phi nen tang tren fee_base (vd 0.08 = 8%), doc tu tham so "platform_fee_rate". */
    BigDecimal getPlatformFeeRate();

    /** So don INVITED toi da dong thoi cho 1 cong viec, doc tu tham so "max_concurrent_invites_per_task". */
    int getMaxConcurrentInvitesPerTask();

    /** So gio truoc khi 1 loi moi tu dong het han, doc tu tham so "invite_expiry_hours". */
    long getInviteExpiryHours();
}
