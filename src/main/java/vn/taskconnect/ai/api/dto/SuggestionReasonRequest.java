package vn.taskconnect.ai.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Yeu cau Groq sinh ly do/diem tru cho mot lo ung vien, dua tren mot boi canh chung
 * (vd mo ta cong viec) va danh sach ung vien voi cac "facts" da rut gon thanh chuoi de doc
 * (vd "distance" -> "1.2 km"). Cau truc chung, khong dung tu vung Task/Tasker de module AI
 * co the tai su dung cho use case goi y khac trong tuong lai ma khong doi API nay.
 *
 * @param contextDescription mo ta boi canh chung cua yeu cau (vd mo ta cong viec can lam)
 * @param candidates danh sach ung vien can Groq danh gia, moi ung vien mang mot bo facts
 */
public record SuggestionReasonRequest(String contextDescription, List<CandidateFacts> candidates) {

    /**
     * Mot ung vien va cac du kien lien quan da duoc quy doi thanh chuoi tieng Viet de doc
     * (vd {"distance": "1.2 km", "priceFit": "trong khoang de xuat"}). candidateId chi la
     * dinh danh de khop lai ket qua Groq tra ve voi dung ung vien, khong mang y nghia nghiep vu.
     */
    public record CandidateFacts(String candidateId, Map<String, String> facts) {
    }
}
