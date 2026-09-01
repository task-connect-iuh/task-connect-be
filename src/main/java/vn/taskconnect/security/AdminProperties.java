package vn.taskconnect.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh admin, doc tu {@code app.admin.*} trong application.yml (anh xa tu bien moi
 * truong ADMIN_SEED_EMAIL qua .env - cung bien dung de seed V10__seed_admin_account.sql).
 *
 * @param superAdminEmail email tai khoan super-admin duy nhat - AuthService so khop email
 *                        nay voi tai khoan dang goi de nhan dien super-admin, khong dung
 *                        role rieng trong schema, xem docs/PROGRESS-ADMIN-MODULE.md.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String superAdminEmail) {
}
