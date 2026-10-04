package vn.taskconnect.ai.api.dto;

/**
 * Mot cap cau hoi lam ro (xem ClarifyingQuestion) + cau tra loi tho cua Poster, dung lam
 * input cho AiFacade.refineTaskDescription - LLM se viet lai thanh doan mo ta lien mach thay
 * vi ghep tho "{key}: {answer}".
 *
 * @param questionText nguyen van cau hoi AI da hoi (ClarifyingQuestion.text)
 * @param answer cau tra loi tho Poster go, co the con thieu dau/viet tat
 */
public record ClarifyingAnswerInput(String questionText, String answer) {
}
