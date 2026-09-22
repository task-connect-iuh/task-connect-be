package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Bat/tat cong tac nhan loi moi truc tiep (INVITED, UC09) cho mot category, dung cho
 * PUT /api/v1/users/me/tasker-skills/{categoryId}/accepts-direct-invites. Dung Boolean (khong
 * phai boolean nguyen thuy) de Bean Validation bat duoc truong hop client quen gui gia tri,
 * thay vi am tham mac dinh false.
 */
public record UpdateAcceptsDirectInvitesRequest(@NotNull Boolean acceptsDirectInvites) {
}
