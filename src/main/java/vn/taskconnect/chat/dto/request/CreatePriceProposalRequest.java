package vn.taskconnect.chat.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Du lieu tao 1 de xuat gia, dung cho POST /api/v1/chat/applications/{applicationId}/price-proposals.
 * Dung lai dung nguong 100.000 d - 50.000.000 d da chot voi nguoi dung cho ngan sach dang viec
 * (xem CreateTaskRequest.budgetAmount) - suy luan hop ly vi de xuat gia la so tien thuong luong
 * cuoi cung cho cung 1 pham vi 5 nhom dich vu, chua co nguong rieng nao khac trong dac ta.
 */
public record CreatePriceProposalRequest(
        @NotNull
        @Min(value = 100_000, message = "Mức giá đề xuất phải từ 100.000 đ trở lên.")
        @Max(value = 50_000_000, message = "Mức giá đề xuất không được vượt quá 50.000.000 đ.")
        Long amount,
        @Size(max = 2000) String note
) {
}
