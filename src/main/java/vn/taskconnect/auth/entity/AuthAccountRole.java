package vn.taskconnect.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.taskconnect.auth.api.AccountRole;

/**
 * Vai tro cua mot tai khoan. Mot tai khoan co the co nhieu dong (vd vua TASK_POSTER
 * vua TASKER). Xem V1__create_auth_tables.sql.
 */
@Entity
@Table(name = "auth_account_roles")
public class AuthAccountRole {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private AccountRole role;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    protected AuthAccountRole() {
        // JPA
    }

    public AuthAccountRole(UUID id, UUID accountId, AccountRole role, Instant grantedAt) {
        this.id = id;
        this.accountId = accountId;
        this.role = role;
        this.grantedAt = grantedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public AccountRole getRole() {
        return role;
    }
}
