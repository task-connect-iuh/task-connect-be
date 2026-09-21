package vn.taskconnect.matching.dto.response;

import java.util.List;
import java.util.UUID;
import vn.taskconnect.user.api.KycStatus;

/**
 * Mot Tasker duoc AI goi y cho mot Task, dung cho GET /api/v1/tasks/{taskId}/suggested-taskers.
 * Doi chieu dung tung field UI trong mockup "TaskConnect - Chot gia & Chat (offline).html"
 * tab "Tasker goi y" - xem bang doi chieu trong plan da duyet. Tra ve toi da 10 phan tu, sap
 * theo finalScore giam dan - FE chi render theo dung thu tu nhan duoc, khong tu sap lai.
 */
public record SuggestedTaskerResponse(
        UUID taskerId,
        String fullName,
        String avatarUrl,
        KycStatus kycStatus,
        int confidence,
        List<String> reasons,
        List<String> concerns,
        boolean lowConfidence,
        double distanceKm,
        Long priceMin,
        Long priceMax,
        // Luon la 0 o dot nay - module Booking/Review chua ton tai nen chua co du lieu "viec
        // da hoan thanh" that su de dem. Khong bia so, xem plan da duyet muc bang doi chieu FE.
        int completedJobsNearby
) {
}
