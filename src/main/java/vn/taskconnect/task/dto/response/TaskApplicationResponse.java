package vn.taskconnect.task.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.task.api.TaskApplicationInitiator;
import vn.taskconnect.task.api.TaskApplicationStatus;

/**
 * Mot don ung tuyen nhin tu phia Poster (danh sach ung vien cua 1 cong viec), dung cho
 * GET /api/v1/tasks/{taskId}/applications. initiatedBy/expiresAt them tu Round B5 de Poster
 * thay duoc loi moi minh da gui (INVITED) va han het han cua no. agreedPriceAmount/
 * pendingProposalAmount them cho man "Ung vien & chot gia" (Round F2 FE) - doc tu
 * task_price_history (module nay so huu) + ChatFacade.hasPendingProposal (Chat moi biet 1 dong
 * NULL con la de xuat dang cho hay la tan du cua 1 lan Tu choi/Thu hoi truoc do, xem
 * TaskApplicationService.priceSnapshotFor). Ca 2 deu co the null (chua tung co de xuat nao/
 * khong co de xuat nao dang cho). hasBooking them (2026-09-28) - don UNG VIEN THANG cua UC11
 * "Chon nguoi nay" GIU NGUYEN status=PENDING (xem Javadoc TaskApplicationService.confirm()), nen
 * FE khong the dua vao status de biet don nao da duoc chon; hasBooking (tu
 * BookingFacade.findByApplicationId) moi la tin hieu dung, dung de an nut "Chon nguoi
 * nay"/"Tu choi" va doi badge trang thai cho ung vien da duoc chon. pendingProposalProposedBy
 * them cho man "Ung vien & chot gia" (yeu cau nguoi dung) - chi khac null khi pendingProposalAmount
 * khac null, cho biet Tasker hay Poster la nguoi tao de xuat dang cho de FE doi nhan/thong bao
 * cho dung chieu. CHI de hien thi - Dong y/Tu choi de xuat van chi lam duoc trong khung chat
 * (PriceProposalCard.tsx), khong phai tren man hinh nay.
 */
public record TaskApplicationResponse(
        UUID id,
        UUID taskerId,
        String taskerName,
        String taskerAvatarUrl,
        String proposedArrivalText,
        String message,
        TaskApplicationStatus status,
        TaskApplicationInitiator initiatedBy,
        Instant expiresAt,
        Instant createdAt,
        Instant respondedAt,
        Long agreedPriceAmount,
        Long pendingProposalAmount,
        TaskApplicationInitiator pendingProposalProposedBy,
        boolean hasBooking
) {
}
