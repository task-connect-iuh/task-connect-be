package vn.taskconnect.booking.api;

/**
 * Phuong thuc thanh toan Poster chon o UC11 "Chon nguoi nay" (quyet dinh nguoi dung 2026-10-02) -
 * quyet dinh he thong giu bao nhieu tien cho booking nay qua PaymentFacade.holdInitial(). Dat o
 * day (booking.api), khong phai payment.api, vi day la thuoc tinh cua CHINH booking ("tien cua
 * booking nay duoc giu kieu gi") - Payment chi can biet SO TIEN phai giu (holdAmount, da tinh san
 * boi BookingFacadeImpl), khong can biet PHUONG THUC nao da tao ra so do.
 */
public enum PaymentMethod {
    /** He thong tam giu 100% gia chot qua escrow - khi giai ngan (ngoai pham vi dot nay) se tra 92% cho Tasker, 8% la phi nen tang. */
    FULL_ESCROW,
    /** He thong chi tam giu 8% phi nen tang - 92% con lai hai ben tu thanh toan truc tiep ngoai he thong, he thong khong quan ly. */
    FEE_ONLY_ESCROW
}
