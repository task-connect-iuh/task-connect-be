package vn.taskconnect.task.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.task.api.TaskStatus;

/**
 * Thong tin cua mot cong viec, dung khi module khac (Matching, Booking...) can doc qua
 * {@link vn.taskconnect.task.api.TaskFacade}. Mo rong tu ban toi gian ban dau (chi id/
 * posterId/categoryId/status) de phuc vu module Matching (goi y Tasker) - them lat/lng
 * (loc ban kinh), budgetAmount (khop gia), scheduledAt (khop lich ranh), title/description
 * (dung lam ngu canh cho AI viet ly do va embedding ngu nghia). Quyet dinh mo rong DTO nay
 * thay vi tao method/DTO song song, vi cac module doc du lieu Task deu can chung mot tap
 * truong toi thieu, tranh nhan doi facade method khong can thiet.
 */
public record TaskSummary(
        UUID id,
        UUID posterId,
        UUID categoryId,
        TaskStatus status,
        String title,
        String description,
        String addressText,
        BigDecimal lat,
        BigDecimal lng,
        Long budgetAmount,
        Instant scheduledAt
) {
}
