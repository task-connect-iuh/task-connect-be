package vn.taskconnect.admin.api;

import java.math.BigDecimal;

/**
 * Be mat cong khai duy nhat cua module Admin. Module khac chi duoc goi qua day, cam import
 * entity trong {@code admin.entity} hoac inject repository cua module Admin. Dot nay chi lam
 * phan doc nguong van hanh (admin_system_parameters) can cho Chat/Task/Booking - cac phan con
 * lai cua module Admin (duyet ho so, khieu nai, dashboard) chua lam.
 */
public interface AdminFacade {

    /** Ty le phi nen tang tren fee_base (vd 0.08 = 8%), doc tu tham so "platform_fee_rate". */
    BigDecimal getPlatformFeeRate();

    /** So don INVITED toi da dong thoi cho 1 cong viec, doc tu tham so "max_concurrent_invites_per_task". */
    int getMaxConcurrentInvitesPerTask();

    /** So gio truoc khi 1 loi moi tu dong het han, doc tu tham so "invite_expiry_hours". */
    long getInviteExpiryHours();

    /** So phut toi da de con thu hoi 1 tin nhan chat, doc tu tham so "chat_message_recall_window_minutes". */
    long getChatMessageRecallWindowMinutes();

    /** So tin nhan toi da duoc ghim dong thoi/kenh, doc tu tham so "chat_max_pinned_messages_per_channel". */
    int getChatMaxPinnedMessagesPerChannel();

    /** So anh toi da/1 tin nhan IMAGE, doc tu tham so "chat_image_max_count_per_message". */
    int getChatImageMaxCountPerMessage();

    /** Dung luong toi da 1 anh (MB), doc tu tham so "chat_image_max_size_mb". */
    long getChatImageMaxSizeMb();

    /** So video toi da/1 tin nhan VIDEO, doc tu tham so "chat_video_max_count_per_message". */
    int getChatVideoMaxCountPerMessage();

    /** Dung luong toi da 1 video (MB), doc tu tham so "chat_video_max_size_mb". */
    long getChatVideoMaxSizeMb();

    /** Thoi luong toi da 1 video (giay), doc tu tham so "chat_video_max_duration_seconds". */
    long getChatVideoMaxDurationSeconds();

    /** So file toi da/1 tin nhan FILE, doc tu tham so "chat_file_max_count_per_message". */
    int getChatFileMaxCountPerMessage();

    /** Dung luong toi da 1 file tai lieu (MB), doc tu tham so "chat_file_max_size_mb". */
    long getChatFileMaxSizeMb();

    /** Thoi luong toi da 1 tin nhan thoai (giay), doc tu tham so "chat_voice_max_duration_seconds". */
    long getChatVoiceMaxDurationSeconds();

    /** Dung luong toi da 1 tin nhan thoai (MB), doc tu tham so "chat_voice_max_size_mb". */
    long getChatVoiceMaxSizeMb();
}
