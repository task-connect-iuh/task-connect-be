package vn.taskconnect.admin.dto.response;

import java.math.BigDecimal;

/**
 * Cac nguong van hanh FE can doc de hien thi (vd widget xem truoc phi luc soan PRICE_PROPOSAL,
 * dac ta muc 4) - dung cho GET /api/v1/system-parameters, moi tai khoan da dang nhap goi duoc.
 */
public record SystemParametersResponse(
        BigDecimal platformFeeRate,
        int maxConcurrentInvitesPerTask,
        long inviteExpiryHours
) {
}
