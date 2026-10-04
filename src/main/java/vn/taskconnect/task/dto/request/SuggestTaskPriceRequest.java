package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Yeu cau goi y muc gia luc dang viec, dung cho POST /api/v1/tasks/suggest-price. title/
 * description la noi dung HIEN TAI tren form (co the tu AI dien tu anh hoac Poster tu go tay,
 * khong phan biet nguon) - dung de embed va so sanh voi cac task tuong tu da chot gia trong
 * qua khu (xem TaskPriceSuggestionService). categoryId bat buoc vi chi so sanh trong CUNG
 * nhom dich vu, khong so sanh cheo category.
 */
public record SuggestTaskPriceRequest(@NotNull UUID categoryId, @NotBlank String title, @NotBlank String description) {
}
