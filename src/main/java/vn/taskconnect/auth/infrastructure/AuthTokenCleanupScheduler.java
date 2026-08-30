package vn.taskconnect.auth.infrastructure;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.taskconnect.auth.service.AuthTokenCleanupService;

/**
 * Kich hoat don dep token het han cua module Auth theo lich co dinh - logic xoa thuc su
 * nam o AuthTokenCleanupService, file nay chi giu phan ky thuat (lich chay).
 */
@Component
public class AuthTokenCleanupScheduler {

    private final AuthTokenCleanupService cleanupService;

    public AuthTokenCleanupScheduler(AuthTokenCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    /**
     * Chay ngay khi app khoi dong (initialDelay = 0), roi lap lai moi 7 ngay ke tu khi
     * lan chay truoc KET THUC (fixedDelayString, khong phai fixedRateString - tranh
     * chong lich neu mot lan don dep bi cham do DB tai cao). Khong de initialDelay = 7
     * ngay: du lieu het han co san tu truoc (vd sau khi seed/test) se phai cho du 7 ngay
     * moi bi don lan dau, du gay kho hieu khi kiem tra thu cong.
     * Chuoi "P7D" la ISO-8601 duration (java.time.Duration.parse), Spring ho tro truc
     * tiep tren *String de khong can hang so kieu long tinh tay tu gio/phut/giay.
     */
    @Scheduled(initialDelay = 0, fixedDelayString = "P7D")
    public void run() {
        cleanupService.cleanupExpiredTokens();
    }
}
