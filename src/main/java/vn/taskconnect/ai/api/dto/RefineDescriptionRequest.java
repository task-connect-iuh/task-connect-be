package vn.taskconnect.ai.api.dto;

import java.util.List;

/**
 * Input cho AiFacade.refineTaskDescription - gop mo ta goc Poster da viet (hoac AI goi y tu
 * anh) voi cac cau tra loi cho clarifyingQuestions (xem ClarifyingAnswerInput), de LLM viet
 * lai thanh MOT doan mo ta tu nhien, du chi tiet cho Tasker hieu dung cong viec, thay vi Poster
 * tu doc cau tra loi tho cua minh ghep vao cuoi o Mo ta.
 *
 * @param originalDescription mo ta hien tai tren form (co the rong neu Poster chua go gi)
 * @param answers danh sach cau hoi + tra loi Poster da dien, chi gom cau DA tra loi (FE loc
 *                 bo cau de trong truoc khi goi)
 */
public record RefineDescriptionRequest(String originalDescription, List<ClarifyingAnswerInput> answers) {
}
