package vn.taskconnect.task.dto.response;

import java.time.Instant;

/**
 * Phan hoi cua POST /tasks/extra-cost-images/upload-url - cung co che voi TaskImageUploadUrlResponse.
 * Client PUT truc tiep len uploadUrl, roi dung publicUrl lam photoUrl cua 1 ExtraCostItemInput.
 */
public record ExtraCostImageUploadUrlResponse(String uploadUrl, String publicUrl, Instant expiresAt) {
}
