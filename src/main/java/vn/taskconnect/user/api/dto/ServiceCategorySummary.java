package vn.taskconnect.user.api.dto;

import java.util.UUID;

/**
 * Thong tin danh muc nhom dich vu, dung khi module khac (Task luc dang cong viec, Matching
 * luc loc theo nhom) can doc qua {@link vn.taskconnect.user.api.UserFacade}.
 */
public record ServiceCategorySummary(
        UUID id,
        String code,
        String name,
        int minExperienceYears
) {
}
