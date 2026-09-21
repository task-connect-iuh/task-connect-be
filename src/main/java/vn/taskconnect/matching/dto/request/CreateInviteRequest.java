package vn.taskconnect.matching.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Yeu cau Poster tao loi moi truc tiep cho mot Tasker cho mot Task cu the. */
public record CreateInviteRequest(@NotNull UUID taskerId) {
}
