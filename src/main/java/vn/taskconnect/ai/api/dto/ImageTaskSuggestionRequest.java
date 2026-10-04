package vn.taskconnect.ai.api.dto;

import java.util.List;

/**
 * Yeu cau Gemini Vision phan tich mot anh minh hoa cong viec, sinh tieu de + mo ta tieng Viet
 * VA phan loai category trong CUNG mot lan goi (tiet kiem 1 luot goi rieng so voi goi lai
 * classifyTaskCategory tren mo ta vua sinh). Tai su dung dung schema CandidateCategory cua
 * CategoryClassificationRequest de dung mot vocab RAG duy nhat cho ca hai luong phan loai
 * (van ban luc submit va anh luc goi y dien form) - xem .claude/rules/15-ai-module.md.
 *
 * @param imageBytes du lieu nhi phan cua anh, da duoc nguoi goi (TaskService) tai ve tu S3 va
 *        xac minh thuoc so huu chinh Poster dang goi (xem TaskImageFetcher) - module AI khong
 *        tu tai anh hay xac thuc quyen so huu
 * @param mimeType kieu MIME cua anh (image/jpeg, image/png, image/webp)
 * @param candidates danh sach danh muc ung vien - buoc retrieval RAG, giong CategoryClassificationRequest
 */
public record ImageTaskSuggestionRequest(byte[] imageBytes, String mimeType,
        List<CategoryClassificationRequest.CandidateCategory> candidates) {
}
