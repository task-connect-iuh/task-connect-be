package vn.taskconnect.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.taskconnect.admin.api.AdminFacade;
import vn.taskconnect.admin.dto.response.SystemParametersResponse;
import vn.taskconnect.common.response.ApiResponse;

/**
 * Endpoint doc cong khai (moi tai khoan da dang nhap) cac nguong van hanh can hien o FE - vd
 * widget xem truoc phi luc soan de xuat gia (dac ta muc 4). Khong co endpoint sua tham so o
 * dot nay (chua co man Admin chinh tham so).
 */
@RestController
@RequestMapping("/api/v1/system-parameters")
public class SystemParametersController {

    private final AdminFacade adminFacade;

    public SystemParametersController(AdminFacade adminFacade) {
        this.adminFacade = adminFacade;
    }

    /** Doc 3 tham so hien can cho Chat/Task: ty le phi nen tang, gioi han moi dong thoi, thoi han het han loi moi. */
    @GetMapping
    public ApiResponse<SystemParametersResponse> get() {
        return ApiResponse.ok(new SystemParametersResponse(adminFacade.getPlatformFeeRate(),
                adminFacade.getMaxConcurrentInvitesPerTask(), adminFacade.getInviteExpiryHours()));
    }
}
