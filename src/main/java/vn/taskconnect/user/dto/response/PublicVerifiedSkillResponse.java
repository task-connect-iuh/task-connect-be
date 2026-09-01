package vn.taskconnect.user.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Mot nhom dich vu da duoc xac minh VERIFIED cua mot Tasker, lo cong khai tren
 * GET /users/{accountId} de nguoi xem ho so (Poster hoac Tasker khac) thay badge "Da xac
 * minh" cho tung nhom dich vu - xem PublicProfileResponse. Khong lo thong tin nhay cam
 * (kinh nghiem, gia, chung chi) - chi categoryId/categoryName/verifiedAt.
 */
public record PublicVerifiedSkillResponse(
        UUID categoryId,
        String categoryName,
        Instant verifiedAt
) {
}
