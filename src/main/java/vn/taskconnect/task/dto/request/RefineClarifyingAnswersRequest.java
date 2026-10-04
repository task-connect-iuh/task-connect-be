package vn.taskconnect.task.dto.request;

import java.util.List;

/**
 * Input cho POST /tasks/refine-description - gop mo ta hien tai tren form voi cac cau tra loi
 * Poster vua dien trong modal "Hoi them" (xem ClarifyAssistantDialog.tsx) thanh MOT doan mo ta
 * hoan chinh do AI viet lai, thay vi FE tu ghep tho "{key}: {answer}." vao cuoi mo ta.
 *
 * @param originalDescription mo ta hien tai tren form, co the rong
 * @param answers danh sach cau hoi + tra loi Poster da dien (chi cau DA tra loi)
 */
public record RefineClarifyingAnswersRequest(String originalDescription, List<AnsweredQuestionRequest> answers) {
}
