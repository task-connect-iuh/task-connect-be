package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Du lieu dang ky ky nang gop nop chung chi cho MOT category, dung cho
 * POST /users/me/tasker-skills (Buoc 6, chi Tasker). fileKey la object key S3 tra ve tu
 * buoc xin presigned upload URL (POST .../certificate-upload-url), khong phai URL day du -
 * TaskerSkillService kiem tra key thuoc dung prefix cua chinh tai khoan + category truoc
 * khi ma hoa va luu. priceMin/priceMax tuy chon (de trong ca hai la hop le) nhung khi CO nhap
 * thi phai nam trong 50.000 d - 10.000.000 d (nguong nghiep vu chot cung voi nguoi dung
 * 2026-09-16, khop PRICE_MIN_VND/PRICE_MAX_VND o TaskerSkillsPage.tsx); rang buoc lien-field
 * "nhap mot thi phai nhap ca hai" va "max > min" kiem o TaskerSkillService.requirePriceRange().
 */
public record SubmitSkillRequest(
        @NotNull UUID categoryId,
        // TINYINT UNSIGNED cho phep toi 255, nhung gioi han thuc te hop ly hon nhieu.
        @Min(0) @Max(60) int yearsExperience,
        // Don vi: dong (VND) nguyen - FE nhan 1000 truoc khi gui vi nguoi dung go don vi nghin.
        @Min(value = 50_000, message = "Giá tối thiểu phải từ 50.000 đ trở lên.")
        @Max(value = 10_000_000, message = "Giá tối thiểu không được vượt quá 10.000.000 đ.")
        Long priceMin,
        @Min(value = 50_000, message = "Giá tối đa phải từ 50.000 đ trở lên.")
        @Max(value = 10_000_000, message = "Giá tối đa không được vượt quá 10.000.000 đ.")
        Long priceMax,
        @NotNull UUID certificateTypeId,
        @Size(max = 100) String certificateNumber,
        @Size(max = 255) String issuingAuthority,
        @PastOrPresent LocalDate issuedDate,
        LocalDate expiryDate,
        @NotBlank @Size(max = 500) String fileKey,
        @Size(max = 500) String experienceProofUrl,
        @Min(0) @Max(60) Integer claimedExperienceYears
) {
}
