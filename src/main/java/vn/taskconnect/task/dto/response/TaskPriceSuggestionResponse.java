package vn.taskconnect.task.dto.response;

/**
 * Ket qua goi y muc gia cho POST /api/v1/tasks/suggest-price - CHI la goi y (dua tren median
 * gia cua cac task tuong tu da chot trong qua khu), Poster xem va sua/xoa tuy y truoc khi dang,
 * khong rang buoc gi. available=false khi khong du du lieu tuong tu (chua du sampleSize toi
 * thieu) hoac AI (embedding) loi/het quota - FE giu nguyen o Ngan sach nhu cu ("thoa thuan"),
 * KHONG chan dang viec.
 *
 * @param available true neu co du du lieu de goi y
 * @param suggestedAmount median gia (don vi dong) cua sampleSize task tuong tu nhat, null neu
 *                          available=false
 * @param sampleSize so task tuong tu thuc su dung de tinh median (>= minSamples cau hinh), 0
 *                     neu available=false - hien cho Poster biet do tin cay dua tren bao nhieu
 *                     du lieu, khong bia mot con so "chac chan"
 */
public record TaskPriceSuggestionResponse(boolean available, Long suggestedAmount, int sampleSize) {

    /** Ket qua khi khong du du lieu tuong tu hoac AI loi/het quota - tat ca truong con lai rong. */
    public static TaskPriceSuggestionResponse unavailable() {
        return new TaskPriceSuggestionResponse(false, null, 0);
    }
}
