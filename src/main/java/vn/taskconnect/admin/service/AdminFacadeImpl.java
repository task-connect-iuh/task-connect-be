package vn.taskconnect.admin.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import vn.taskconnect.admin.api.AdminFacade;
import vn.taskconnect.admin.entity.AdminSystemParameter;
import vn.taskconnect.admin.repository.AdminSystemParameterRepository;

/** Trien khai duy nhat cua AdminFacade - khong module nao khac trong admin duoc implements interface nay. */
@Service
class AdminFacadeImpl implements AdminFacade {

    private static final String KEY_PLATFORM_FEE_RATE = "platform_fee_rate";
    private static final String KEY_MAX_CONCURRENT_INVITES_PER_TASK = "max_concurrent_invites_per_task";
    private static final String KEY_INVITE_EXPIRY_HOURS = "invite_expiry_hours";

    private final AdminSystemParameterRepository repository;

    AdminFacadeImpl(AdminSystemParameterRepository repository) {
        this.repository = repository;
    }

    /** Doc va parse "platform_fee_rate" thanh BigDecimal - nem loi ky thuat neu seed bi thieu/hong. */
    @Override
    public BigDecimal getPlatformFeeRate() {
        return new BigDecimal(requireParam(KEY_PLATFORM_FEE_RATE));
    }

    /** Doc va parse "max_concurrent_invites_per_task" thanh so nguyen. */
    @Override
    public int getMaxConcurrentInvitesPerTask() {
        return Integer.parseInt(requireParam(KEY_MAX_CONCURRENT_INVITES_PER_TASK));
    }

    /** Doc va parse "invite_expiry_hours" thanh so nguyen dai. */
    @Override
    public long getInviteExpiryHours() {
        return Long.parseLong(requireParam(KEY_INVITE_EXPIRY_HOURS));
    }

    /** Doc gia tri chuoi tho cua 1 tham so - nem IllegalStateException neu chua seed (loi cau hinh, khong phai loi nguoi dung). */
    private String requireParam(String key) {
        return repository.findByParamKey(key)
                .map(AdminSystemParameter::getParamValue)
                .orElseThrow(() -> new IllegalStateException("Thieu tham so he thong: " + key));
    }
}
