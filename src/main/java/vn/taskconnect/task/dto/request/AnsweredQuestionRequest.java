package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Mot cap cau hoi lam ro (xem ClarifyingQuestionResponse) + cau tra loi Poster da go, dung lam
 * input cho POST /tasks/refine-description. FE chi gui cau DA tra loi (da loc bo cau de trong).
 *
 * @param questionText nguyen van cau hoi AI da hoi (ClarifyingQuestionResponse.text)
 * @param answer cau tra loi tho Poster go
 */
public record AnsweredQuestionRequest(@NotBlank String questionText, @NotBlank String answer) {
}
