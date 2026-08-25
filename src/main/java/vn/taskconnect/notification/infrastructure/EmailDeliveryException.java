package vn.taskconnect.notification.infrastructure;

/**
 * Boc moi loi ky thuat cua tang gui mail (SMTP timeout, tu choi xac thuc, dia chi
 * nguoi gui chua xac minh...) thanh mot loai unchecked duy nhat, de tang goi
 * (NotificationFacadeImpl) chi can bat mot kieu thay vi liet ke tung checked exception
 * cua JavaMail.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(Throwable cause) {
        super(cause);
    }
}
