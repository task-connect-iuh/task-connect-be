package vn.taskconnect.task.entity;

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
import vn.taskconnect.task.api.TaskApplicationInitiator;
import vn.taskconnect.task.api.TaskApplicationStatus;

/**
 * Mot don ung tuyen cua Tasker vao mot cong viec (UC09/UC10/UC11/UC16). Xem
 * V19__create_task_application_table.sql va V27__extend_task_application_status_and_negotiation_fields.sql.
 * Chuyen trang thai khong tu validate ben trong entity (giong Task.java/KycVerification.java) -
 * dieu kien hop le (task dang OPEN, don dang PENDING...) kiem tra o TaskApplicationService.
 */
@Entity
@Table(name = "task_applications")
public class TaskApplication {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "task_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID taskId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "tasker_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID taskerId;

    // Da bo khoi form ung tuyen (yeu cau nguoi dung) - giu cot nullable de tuong thich nguoc,
    // khong bat buoc nua.
    @Column(name = "proposed_arrival_text", length = 200, updatable = false)
    private String proposedArrivalText;

    @Column(name = "message", columnDefinition = "TEXT", updatable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskApplicationStatus status;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "proposed_price")
    private Long proposedPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiated_by", nullable = false, length = 10)
    private TaskApplicationInitiator initiatedBy;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected TaskApplication() {
        // JPA
    }

    /** Tao mot don ung tuyen moi do chinh Tasker gui (UC10), luon bat dau o trang thai PENDING. */
    public static TaskApplication submit(UUID id, UUID taskId, UUID taskerId, String proposedArrivalText,
            String message, Instant now) {
        TaskApplication application = new TaskApplication();
        application.id = id;
        application.taskId = taskId;
        application.taskerId = taskerId;
        application.proposedArrivalText = proposedArrivalText;
        application.message = message;
        application.status = TaskApplicationStatus.PENDING;
        application.initiatedBy = TaskApplicationInitiator.TASKER;
        application.createdAt = now;
        return application;
    }

    /**
     * Poster xac nhan don nay (nut Xac nhan thu cong cu, GIU LAI ngoai dac ta theo quyet dinh
     * cua nguoi dung) - chuyen ACCEPTED, ghi nhan thoi diem phan hoi. KHONG dung cho luong
     * "Chon nguoi nay" o UC11 (TaskConnect_Chat_ImplementationSpec.md muc 7) - ung vien thang
     * cuoc o UC11 giu nguyen PENDING, xem TaskApplicationService.confirm().
     */
    public void accept(Instant now) {
        this.status = TaskApplicationStatus.ACCEPTED;
        this.respondedAt = now;
    }

    /**
     * Poster tu choi thu cong don nay (nut Tu choi, GIU LAI ngoai dac ta theo quyet dinh cua
     * nguoi dung) - chuyen REJECTED, ghi nhan thoi diem phan hoi. Khac REJECTED_AUTO (he thong
     * tu dong tu choi cac don con lai khi UC11 chon xong nguoi thang).
     */
    public void reject(Instant now) {
        this.status = TaskApplicationStatus.REJECTED;
        this.respondedAt = now;
    }

    /**
     * KHONG CON DUOC GOI TU Round B4 tro di - giu lai method nay chi de khong pha code cu
     * truoc khi TaskApplicationService.confirm() duoc viet lai. TaskConnect_Chat_ImplementationSpec.md
     * muc 5 quy dinh cascade UC11 phai la REJECTED_AUTO (xem rejectAuto()), khong phai
     * NEEDS_RECONFIRM. Se xoa method nay khi Round B4 hoan tat.
     */
    public void markNeedsReconfirm(Instant now) {
        this.status = TaskApplicationStatus.NEEDS_RECONFIRM;
        this.respondedAt = now;
    }

    /**
     * He thong tu dong tu choi don nay vi mot ung vien khac cua cung task da duoc chon o UC11
     * (TaskConnect_Chat_ImplementationSpec.md muc 5 va 7.4) - chuyen REJECTED_AUTO, ghi nhan
     * thoi diem phan hoi. Khac reject() (Poster chu dong tu choi thu cong khi task con mo).
     */
    public void rejectAuto(Instant now) {
        this.status = TaskApplicationStatus.REJECTED_AUTO;
        this.respondedAt = now;
    }

    /**
     * Tasker rut lui mot don dang PENDING hoac INQUIRING (nhan/SYSTEM message hien thi khac
     * nhau theo nguon goc, xem dac ta muc 5, nhung dung chung 1 gia tri enum WITHDRAWN).
     */
    public void withdraw(Instant now) {
        this.status = TaskApplicationStatus.WITHDRAWN;
        this.respondedAt = now;
    }

    /** Tasker tu choi mot loi moi truc tiep (INVITED) - chan moi lai vinh vien cho cung task. */
    public void declineInvite(Instant now) {
        this.status = TaskApplicationStatus.DECLINED;
        this.respondedAt = now;
        this.expiresAt = null;
    }

    /** Tasker nhan mot loi moi truc tiep (INVITED) - chuyen PENDING, xoa han het han loi moi. */
    public void acceptInvite(Instant now) {
        this.status = TaskApplicationStatus.PENDING;
        this.respondedAt = now;
        this.expiresAt = null;
    }

    /** Tien trinh nen (InviteExpirySweeperJob) danh dau 1 loi moi qua han vi Tasker im lang. */
    public void expireInvite(Instant now) {
        this.status = TaskApplicationStatus.INVITE_EXPIRED;
        this.respondedAt = now;
    }

    /**
     * Xoa han het han loi moi ve NULL ngay khi Tasker co bat ky phan hoi nao trong channel (gui
     * TEXT, tao/chap nhan PRICE_PROPOSAL) - lam loi moi khong con bi tien trinh nen tu dong het
     * han nua, chi ket thuc qua hanh dong ro rang (dac ta muc 8). Khong doi status.
     */
    public void clearInviteExpiry() {
        this.expiresAt = null;
    }

    /** Ghi nhan gia moi ca 2 ben da dong y (goi khi 1 PRICE_PROPOSAL duoc Dong y). */
    public void updateProposedPrice(Long proposedPrice) {
        this.proposedPrice = proposedPrice;
    }

    /**
     * Poster moi truc tiep Tasker nay nhan cong viec (UC09) - luon kem han het han. Gia de nghi
     * kem theo (neu co) KHONG luu vao proposedPrice ngay (cot nay chi phan anh muc gia CA HAI
     * BEN da dong y, xem updateProposedPrice()) - thay vao do di qua kenh chat nhu 1
     * PRICE_PROPOSAL binh thuong (TaskApplicationService.invite() goi ChatFacade rieng).
     */
    public static TaskApplication invite(UUID id, UUID taskId, UUID taskerId, String message, Instant now,
            Instant expiresAt) {
        TaskApplication application = new TaskApplication();
        application.id = id;
        application.taskId = taskId;
        application.taskerId = taskerId;
        application.message = message;
        application.status = TaskApplicationStatus.INVITED;
        application.initiatedBy = TaskApplicationInitiator.POSTER;
        application.expiresAt = expiresAt;
        application.createdAt = now;
        return application;
    }

    /** Tasker bam "Nhan tin hoi them" - tu dong tao don INQUIRING, khong het han (UC16 muc 2). */
    public static TaskApplication inquire(UUID id, UUID taskId, UUID taskerId, String message, Instant now) {
        TaskApplication application = new TaskApplication();
        application.id = id;
        application.taskId = taskId;
        application.taskerId = taskerId;
        application.message = message;
        application.status = TaskApplicationStatus.INQUIRING;
        application.initiatedBy = TaskApplicationInitiator.TASKER;
        application.createdAt = now;
        return application;
    }

    /**
     * 1 de xuat gia duoc Dong y trong luc don dang INQUIRING - tu dong nang cap PENDING (khong
     * can thao tac rieng, xem dac ta muc 3).
     */
    public void upgradeToPendingFromInquiring() {
        this.status = TaskApplicationStatus.PENDING;
    }

    /** Id noi bo cua don ung tuyen nay. */
    public UUID getId() {
        return id;
    }

    /** Id cong viec duoc ung tuyen. */
    public UUID getTaskId() {
        return taskId;
    }

    /** Id tai khoan Tasker da gui don. */
    public UUID getTaskerId() {
        return taskerId;
    }

    /** Thoi gian Tasker de xuat toi lam, van ban tu do nguoi dung tu nhap. */
    public String getProposedArrivalText() {
        return proposedArrivalText;
    }

    /** Loi nhan ngan Tasker gui kem don ung tuyen, null neu khong nhap. */
    public String getMessage() {
        return message;
    }

    /** Trang thai hien tai cua don ung tuyen. */
    public TaskApplicationStatus getStatus() {
        return status;
    }

    /** Muc gia gan nhat ca 2 ben da dong y qua PRICE_PROPOSAL (don vi dong), null neu chua co. */
    public Long getProposedPrice() {
        return proposedPrice;
    }

    /** Ai khoi tao don nay - TASKER (tu ung tuyen/hoi them) hay POSTER (moi truc tiep). */
    public TaskApplicationInitiator getInitiatedBy() {
        return initiatedBy;
    }

    /** Han het han loi moi, chi co gia tri khi status = INVITED va Tasker chua phan hoi gi trong channel. */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /** Thoi diem gui don, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem Poster phan hoi (xac nhan/tu choi) hoac tu dong chuyen NEEDS_RECONFIRM, null neu con PENDING. */
    public Instant getRespondedAt() {
        return respondedAt;
    }
}
