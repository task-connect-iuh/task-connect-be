package vn.taskconnect.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.auth.api.AccountStatus;
import vn.taskconnect.auth.repository.AuthAccountRepository;

/**
 * Don dep tai khoan UNVERIFIED bo do - nguoi dang ky khong bao gio hoan tat xac minh
 * email (go sai email, doi y, spam bot...). Khac AuthTokenCleanupService (chi xoa du
 * lieu phu da het han), o day xoa han tai khoan goc sau mot khoang thoi gian an, tranh
 * auth_accounts phinh vo han vi nhung dong khong bao gio dung toi duoc (login()/register()
 * deu da chan UNVERIFIED, tai khoan nay vinh vien khong the su dung neu khong xac minh).
 *
 * <p>Gia dinh hien tai: chua module nao (User...) tao du lieu gan voi mot tai khoan truoc
 * khi tai khoan do ACTIVE, nen xoa an toan. FK tu user.* toi auth_accounts.id (xem
 * V2__create_user_tables.sql) khong khai bao ON DELETE CASCADE (mac dinh RESTRICT) - neu
 * sau nay co module nao tao du lieu gan voi tai khoan con UNVERIFIED, job nay se bat dau
 * nem loi FK khi xoa, can xu ly lai luc do.
 */
@Service
public class AuthAccountCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AuthAccountCleanupService.class);

    /**
     * TODO: nguong nay phai doc tu admin.system_parameters (xem
     * .claude/rules/02-source-of-truth.md - "Ngưỡng nghiệp vụ không bao giờ hardcode").
     * Hardcode tam vi module Admin chua duoc tao, gia tri 24 gio da thong nhat truc tiep
     * voi nguoi dung. Chuyen sang doc tu DB ngay khi Admin co bang nay.
     */
    private static final Duration UNVERIFIED_ACCOUNT_TTL = Duration.ofHours(24);

    private final AuthAccountRepository accountRepository;
    private final Clock clock;

    public AuthAccountCleanupService(AuthAccountRepository accountRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    /**
     * Xoa moi tai khoan con UNVERIFIED duoc tao truoc moc {@code now - 24 gio}. Chi xet
     * created_at, khong quan tam da tung gui/nhan lai OTP bao nhieu lan - tai khoan da qua
     * 24 gio ma van chua xac minh duoc coi la bo do, khong con ly do giu lai.
     */
    @Transactional
    public void cleanupUnverifiedAccounts() {
        Instant cutoff = clock.instant().minus(UNVERIFIED_ACCOUNT_TTL);
        int deleted = accountRepository.deleteByStatusAndCreatedAtBefore(AccountStatus.UNVERIFIED, cutoff);
        log.info("Don dep tai khoan Auth: xoa {} tai khoan UNVERIFIED qua han 24 gio chua xac minh", deleted);
    }
}
