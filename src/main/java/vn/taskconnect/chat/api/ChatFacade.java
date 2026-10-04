package vn.taskconnect.chat.api;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Be mat cong khai duy nhat cua module Chat. Module khac chi duoc goi qua day, cam import
 * entity trong {@code chat.entity} hoac inject repository cua module Chat.
 */
public interface ChatFacade {

    /**
     * Tao kenh + SYSTEM message mo dau + tin nhan TEXT dau tien (chinh cau hoi cua Tasker) -
     * dung cho "Nhan tin hoi them" (goi tu TaskApplicationService ngay sau khi tao don
     * INQUIRING, CUNG 1 transaction - xem dac ta muc 2). Khong lam gi neu kenh da ton tai
     * (phong ho goi lai, khong nen xay ra voi 1 don INQUIRING vua tao).
     */
    void openChannelWithFirstInquiryMessage(UUID applicationId, UUID taskerAccountId, String systemMessageBody,
            String inquiryMessageText, Instant createdAt);

    /**
     * Dong kenh cua 1 application (neu co va dang OPEN) kem 1 SYSTEM message nêu ly do - dung
     * cho withdraw/decline/reject/reject-auto/invite-expired (dac ta muc 6). Khong lam gi neu
     * chua co kenh hoac kenh da CLOSED tu truoc (tranh sinh 2 SYSTEM message dong kenh).
     */
    void closeChannelIfExists(UUID applicationId, String systemMessageBody);

    /**
     * Gui 1 SYSTEM message vao kenh cua application (neu co va dang OPEN), KHONG dong kenh -
     * dung khi Tasker nang cap thang 1 don INQUIRING len PENDING (khac closeChannelIfExists,
     * kenh van tiep tuc mo). Khong lam gi neu chua co kenh hoac kenh da CLOSED.
     */
    void postSystemMessageIfOpen(UUID applicationId, String systemMessageBody);

    /**
     * Nhu postSystemMessageIfOpen() nhung dinh kem id cua 1 su kien Poster sua cong viec (UC07)
     * de FE hien nut "Xem chi tiết thay đổi" tren dung tin nay - dung boi Task.updateTask().
     */
    void postTaskEditSystemMessageIfOpen(UUID applicationId, String systemMessageBody, UUID taskEditEventId);

    /**
     * Gui 1 tin nhan EXTRA_COST_BATCH (the "Chi phí phát sinh" hien truc tiep trong khung chat,
     * giong PRICE_PROPOSAL) vao kenh cua application neu dang OPEN - dung boi
     * TaskExtraCostService.submit() ngay sau khi Tasker dang xong 1 batch (bo sung 2026-10-03,
     * thay the cho 1 dong SYSTEM text truoc day). Khong lam gi neu chua co kenh hoac kenh da
     * CLOSED.
     */
    void postExtraCostBatchMessageIfOpen(UUID applicationId, UUID taskerAccountId, UUID extraCostBatchId);

    /**
     * Co ton tai 1 message dung loai (vd PRICE_PROPOSAL) dang PROPOSED (chua xu ly) cho
     * application nay khong - dung boi Task (proposePrice() de chan tao them de xuat moi, va
     * confirm() UC11 o Round B4 de khoa nut "Chon nguoi nay").
     */
    boolean hasPendingProposal(UUID applicationId, ChatMessageType type);

    /**
     * Trang thai xu ly (PROPOSED/ACCEPTED/REJECTED) cua tung dong task_price_history, tra ve
     * theo dung id da truyen vao - dung cho man "Lich su gia"
     * (TaskApplicationService.listPriceHistory()) de phan biet dung "con dang cho quyet dinh"
     * voi "da bi Tu choi/Thu hoi", 2 truong hop ma rieng bang task_price_history khong the phan
     * biet (xem Javadoc TaskApplicationService.priceSnapshotFor). Id nao khong co dong
     * PRICE_PROPOSAL nao trong chat_messages (khong nen xay ra - moi dong task_price_history
     * luon di kem dung 1 tin nhan de xuat, xem TaskPriceNegotiationService.proposePrice()) thi
     * khong co mat trong map tra ve.
     */
    Map<UUID, ProposalStatus> findProposalStatusesByPriceHistoryIds(Set<UUID> priceHistoryIds);

    /**
     * Gan booking vao kenh cua application (neu co) ngay sau khi UC11 chon xong nguoi thang -
     * dac ta muc 1 (chat_channels.booking_id). Khong lam gi neu chua co kenh nao.
     */
    void attachBooking(UUID applicationId, UUID bookingId);

    /**
     * Lazy-create kenh (neu chua co - ung vien co the chua tung nhan tin gi truoc khi duoc chon)
     * + gui 1 SYSTEM message bao ung vien THANG khi Poster "Chon nguoi nay" (UC11), KHONG dong
     * kenh (khac closeChannelIfExists danh cho cac ung vien thua) - kenh tiep tuc OPEN dung y
     * nghia "Dang thuc hien".
     */
    void notifyApplicationConfirmed(UUID applicationId, String systemMessageBody, Instant now);

    /**
     * Tao kenh cho 1 loi moi truc tiep (UC09) kem 1 SYSTEM message mo dau, va them 1
     * PRICE_PROPOSAL neu Poster co dinh kem gia (proposedAmount khac null) - dac ta muc 2. Khac
     * openChannelWithFirstInquiryMessage: khong co tin nhan TEXT tu do nao. Moi loi moi luon
     * gan voi 1 applicationId MOI HOAN TOAN (dac ta muc 8 ban cap nhat 2026-09-17 - khong con
     * tai su dung dong/kenh cu cho loi moi lai), nen khong can nhanh "mo lai kenh da CLOSED".
     */
    void openChannelForInvite(UUID applicationId, UUID posterAccountId, String systemMessageBody,
            Long proposedAmount, String proposalNote, Instant now);

    /**
     * Tao kenh cho 1 don ung tuyen thang (UC10, yeu cau nguoi dung 2026-09-30) kem 1 SYSTEM
     * message mo dau, va 1 PRICE_PROPOSAL (qua TaskFacade.proposePrice, giong het luong thuong
     * luong gia binh thuong) vi Tasker luon dinh kem gia khi goi method nay - khac
     * openChannelForInvite (proposedAmount o do co the null vi Poster khong bat buoc de nghi
     * gia luc moi). Dung khi Tasker chon "De nghi mot muc khac" o form Gui ung tuyen thay vi
     * chap nhan nguyen ngan sach nguoi dang dua ra.
     */
    void openChannelForApplyWithPriceProposal(UUID applicationId, UUID taskerAccountId, String systemMessageBody,
            long proposedAmount, String proposalNote, Instant now);
}
