package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Du lieu nop ho so xac minh danh tinh, dung cho POST /users/me/kyc-verifications.
 * idCardFrontKey/idCardBackKey la object key S3 tra ve tu buoc xin presigned upload URL
 * (POST .../upload-url), khong phai URL day du - KycVerificationService kiem tra key thuoc
 * dung prefix cua chinh tai khoan truoc khi ma hoa va luu.
 */
public record SubmitKycRequest(
        @NotBlank @Size(max = 150) String fullNameOnId,
        // CCCD 12 so (chuan hien hanh) hoac CMND 9 so (giay to cu van con luu hanh).
        @NotBlank @Pattern(regexp = "\\d{9}|\\d{12}", message = "Số CCCD/CMND phải gồm 9 hoặc 12 chữ số.")
        String idNumber,
        @NotBlank @Size(max = 500) String idCardFrontKey,
        @NotBlank @Size(max = 500) String idCardBackKey
) {
}
