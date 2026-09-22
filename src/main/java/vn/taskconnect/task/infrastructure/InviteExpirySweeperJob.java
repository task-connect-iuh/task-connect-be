package vn.taskconnect.task.infrastructure;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.taskconnect.task.service.TaskApplicationService;

/**
 * Kich hoat quet loi moi truc tiep (INVITED, UC09) qua han theo lich co dinh - logic that nam o
 * TaskApplicationService.sweepExpiredInvites(), file nay chi giu phan ky thuat (lich chay),
 * cung mau AuthTokenCleanupScheduler (module Auth).
 */
@Component
public class InviteExpirySweeperJob {

    private final TaskApplicationService applicationService;

    public InviteExpirySweeperJob(TaskApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * Chay ngay khi app khoi dong (initialDelay = 0), lap lai moi 5 phut ke tu khi lan truoc
     * KET THUC (fixedDelayString, tranh chong lich neu mot lan quet bi cham do DB tai cao). 5
     * phut la con so SUY LUAN (dac ta muc 8 khong quy dinh khoang cu the) - du de nguoi dung
     * khong cho qua lau sau khi loi moi thuc su het han, xem docs/PROGRESS-CHAT-MODULE.md.
     */
    @Scheduled(initialDelay = 0, fixedDelayString = "PT5M")
    public void run() {
        applicationService.sweepExpiredInvites();
    }
}
