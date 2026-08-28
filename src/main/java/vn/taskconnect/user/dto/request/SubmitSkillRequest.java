package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Du lieu dang ky ky nang gop nop chung chi cho MOT category, dung cho
 * POST /users/me/tasker-skills (Buoc 6, chi Tasker). fileKey la object key S3 tra ve tu
 * buoc xin presigned upload URL (POST .../certificate-upload-url), khong phai URL day du -
 * TaskerSkillService kiem tra key thuoc dung prefix cua chinh tai khoan + category truoc
 * khi ma hoa va luu.
 */
public record SubmitSkillRequest(
        @NotNull UUID categoryId,
        // TINYINT UNSIGNED cho phep toi 255, nhung gioi han thuc te hop ly hon nhieu.
        @Min(0) @Max(60) int yearsExperience,
        @PositiveOrZero Long priceMin,
        @PositiveOrZero Long priceMax,
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
