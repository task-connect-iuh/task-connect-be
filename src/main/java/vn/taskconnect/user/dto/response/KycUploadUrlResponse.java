package vn.taskconnect.user.dto.response;

import java.time.Instant;

/**
 * Phan hoi cua POST /users/me/kyc-verifications/upload-url: uploadUrl de client PUT truc
 * tiep anh CCCD len S3 (dung mot lan, het han o expiresAt). Khac AvatarUploadUrlResponse -
 * khong co publicUrl vi anh CCCD la du lieu rieng tu, khong public-read; objectKey phai
 * duoc client gui lai nguyen ven trong SubmitKycRequest de backend ma hoa va luu.
 */
public record KycUploadUrlResponse(String uploadUrl, String objectKey, Instant expiresAt) {
}
