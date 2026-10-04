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
    private static final String KEY_CHAT_MESSAGE_RECALL_WINDOW_MINUTES = "chat_message_recall_window_minutes";
    private static final String KEY_CHAT_MAX_PINNED_MESSAGES_PER_CHANNEL = "chat_max_pinned_messages_per_channel";
    private static final String KEY_CHAT_IMAGE_MAX_COUNT_PER_MESSAGE = "chat_image_max_count_per_message";
    private static final String KEY_CHAT_IMAGE_MAX_SIZE_MB = "chat_image_max_size_mb";
    private static final String KEY_CHAT_VIDEO_MAX_COUNT_PER_MESSAGE = "chat_video_max_count_per_message";
    private static final String KEY_CHAT_VIDEO_MAX_SIZE_MB = "chat_video_max_size_mb";
    private static final String KEY_CHAT_VIDEO_MAX_DURATION_SECONDS = "chat_video_max_duration_seconds";
    private static final String KEY_CHAT_FILE_MAX_COUNT_PER_MESSAGE = "chat_file_max_count_per_message";
    private static final String KEY_CHAT_FILE_MAX_SIZE_MB = "chat_file_max_size_mb";
    private static final String KEY_CHAT_VOICE_MAX_DURATION_SECONDS = "chat_voice_max_duration_seconds";
    private static final String KEY_CHAT_VOICE_MAX_SIZE_MB = "chat_voice_max_size_mb";

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

    /** Doc va parse "chat_message_recall_window_minutes" thanh so nguyen dai. */
    @Override
    public long getChatMessageRecallWindowMinutes() {
        return Long.parseLong(requireParam(KEY_CHAT_MESSAGE_RECALL_WINDOW_MINUTES));
    }

    /** Doc va parse "chat_max_pinned_messages_per_channel" thanh so nguyen. */
    @Override
    public int getChatMaxPinnedMessagesPerChannel() {
        return Integer.parseInt(requireParam(KEY_CHAT_MAX_PINNED_MESSAGES_PER_CHANNEL));
    }

    /** Doc va parse "chat_image_max_count_per_message" thanh so nguyen. */
    @Override
    public int getChatImageMaxCountPerMessage() {
        return Integer.parseInt(requireParam(KEY_CHAT_IMAGE_MAX_COUNT_PER_MESSAGE));
    }

    /** Doc va parse "chat_image_max_size_mb" thanh so nguyen dai. */
    @Override
    public long getChatImageMaxSizeMb() {
        return Long.parseLong(requireParam(KEY_CHAT_IMAGE_MAX_SIZE_MB));
    }

    /** Doc va parse "chat_video_max_count_per_message" thanh so nguyen. */
    @Override
    public int getChatVideoMaxCountPerMessage() {
        return Integer.parseInt(requireParam(KEY_CHAT_VIDEO_MAX_COUNT_PER_MESSAGE));
    }

    /** Doc va parse "chat_video_max_size_mb" thanh so nguyen dai. */
    @Override
    public long getChatVideoMaxSizeMb() {
        return Long.parseLong(requireParam(KEY_CHAT_VIDEO_MAX_SIZE_MB));
    }

    /** Doc va parse "chat_video_max_duration_seconds" thanh so nguyen dai. */
    @Override
    public long getChatVideoMaxDurationSeconds() {
        return Long.parseLong(requireParam(KEY_CHAT_VIDEO_MAX_DURATION_SECONDS));
    }

    /** Doc va parse "chat_file_max_count_per_message" thanh so nguyen. */
    @Override
    public int getChatFileMaxCountPerMessage() {
        return Integer.parseInt(requireParam(KEY_CHAT_FILE_MAX_COUNT_PER_MESSAGE));
    }

    /** Doc va parse "chat_file_max_size_mb" thanh so nguyen dai. */
    @Override
    public long getChatFileMaxSizeMb() {
        return Long.parseLong(requireParam(KEY_CHAT_FILE_MAX_SIZE_MB));
    }

    /** Doc va parse "chat_voice_max_duration_seconds" thanh so nguyen dai. */
    @Override
    public long getChatVoiceMaxDurationSeconds() {
        return Long.parseLong(requireParam(KEY_CHAT_VOICE_MAX_DURATION_SECONDS));
    }

    /** Doc va parse "chat_voice_max_size_mb" thanh so nguyen dai. */
    @Override
    public long getChatVoiceMaxSizeMb() {
        return Long.parseLong(requireParam(KEY_CHAT_VOICE_MAX_SIZE_MB));
    }

    /** Doc gia tri chuoi tho cua 1 tham so - nem IllegalStateException neu chua seed (loi cau hinh, khong phai loi nguoi dung). */
    private String requireParam(String key) {
        return repository.findByParamKey(key)
                .map(AdminSystemParameter::getParamValue)
                .orElseThrow(() -> new IllegalStateException("Thieu tham so he thong: " + key));
    }
}
