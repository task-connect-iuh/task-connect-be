package vn.taskconnect.ai.api.dto;

/**
 * Mot cau hoi lam ro do Gemini Vision sinh ra khi khong khop ro danh muc nao tu anh (outcome
 * OTHER trong ImageTaskSuggestionResult.category) - xem Javadoc ImageTaskSuggestionResult. Hoi
 * ve chi tiet CHI nguoi dang o hien truong moi biet (am thanh, thoi diem, dien bien...), KHONG
 * hoi lai nhung gi anh da the hien ro.
 *
 * @param key nhan ngan 2-5 tu tom tat chu de cau hoi (vd "Tieng nuoc chay"), dung de FE ghep
 *            cau tra loi vao mo ta theo dang "{key}: {answer}"
 * @param text cau hoi day du bang tieng Viet, tu nhien, lich su
 * @param placeholder vi du cau tra loi ngan de goi y Poster cach tra loi
 */
public record ClarifyingQuestion(String key, String text, String placeholder) {
}
