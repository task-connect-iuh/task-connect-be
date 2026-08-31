package vn.taskconnect.auth.infrastructure;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.taskconnect.auth.service.AuthAccountCleanupService;

/**
 * Kich hoat don dep tai khoan UNVERIFIED qua han theo lich co dinh - logic xoa thuc su
 * nam o AuthAccountCleanupService, file nay chi giu phan ky thuat (lich chay).
 */
@Component
public class AuthAccountCleanupScheduler {

    private final AuthAccountCleanupService cleanupService;

    public AuthAccountCleanupScheduler(AuthAccountCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    /**
     * Chay ngay khi app khoi dong (initialDelay = 0) roi lap lai moi 1 ngay ke tu khi lan
     * chay truoc KET THUC (fixedDelayString, khong phai fixedRateString - ly do giong het
     * AuthTokenCleanupScheduler: tranh chong lich neu mot lan don dep bi cham do DB tai
     * cao). Chu ky ngan hon nhieu so voi AuthTokenCleanupScheduler (7 ngay): nguong xoa o
     * day la 24 gio (UNVERIFIED_ACCOUNT_TTL), chay hang ngay de tai khoan qua han khong bi
     * treo lai gan them ca tuan moi thuc su bi don, giu dung tinh than "qua han 24 gio".
     */
    @Scheduled(initialDelay = 0, fixedDelayString = "P1D")
    public void run() {
        cleanupService.cleanupUnverifiedAccounts();
    }
}
