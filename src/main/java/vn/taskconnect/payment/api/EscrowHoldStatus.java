package vn.taskconnect.payment.api;

/**
 * Trang thai 1 ban ghi tam giu tien (payment_escrow_holds). CHI CO DUNG 1 GIA TRI - KHONG PHAI
 * THIEU SOT, cung tinh than voi BookingStatus (xem booking.api.BookingStatus). Dot nay
 * (quyet dinh nguoi dung 2026-10-02) nap tien la GIA LAP va luon thanh cong ngay trong 1
 * transaction, nen PENDING khong bao gio quan sat duoc - khong khai bao de tranh gia tri "chet"
 * khong co code path nao set toi. Se mo rong them PENDING/RELEASED/REFUNDED/FAILED bang 1 thay
 * doi rieng khi Sepay that duoc tich hop va luong giai ngan/hoan tien duoc hien thuc, kem
 * migration fix-forward noi rong CHECK constraint tuong ung (xem V48).
 */
public enum EscrowHoldStatus {
    HELD
}
