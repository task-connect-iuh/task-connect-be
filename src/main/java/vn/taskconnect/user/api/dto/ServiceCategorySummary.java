package vn.taskconnect.user.api.dto;

import java.util.UUID;

/**
 * Thong tin danh muc nhom dich vu, dung khi module khac (Task luc dang cong viec, Matching
 * luc loc theo nhom) can doc qua {@link vn.taskconnect.user.api.UserFacade}.
 *
 * <p>description/keywords la kho tri thuc RAG cho module AI phan loai cong viec (xem
 * .claude/rules/15-ai-module.md) - Task doc qua day de ghep ngu canh gui AI, khong tu JOIN
 * bang user_service_categories.
 */
public record ServiceCategorySummary(
        UUID id,
        String code,
        String name,
        String description,
        String keywords,
        int minExperienceYears
) {
}
