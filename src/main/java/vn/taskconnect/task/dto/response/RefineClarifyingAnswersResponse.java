package vn.taskconnect.task.dto.response;

/**
 * Ket qua goi y mo ta da duoc AI viet lai tu POST /tasks/refine-description - CHI la goi y,
 * Poster van thay ngay tren o Mo ta va sua tuy y truoc khi dang.
 *
 * @param available false neu AI khong viet lai duoc (het quota/loi mang) - FE tu fallback ve
 *                   cach ghep tho "{questionText}: {answer}." vao cuoi mo ta hien tai
 * @param refinedDescription doan mo ta hoan chinh AI da viet lai, null neu available=false
 */
public record RefineClarifyingAnswersResponse(boolean available, String refinedDescription) {

    /** Ket qua khi AI khong viet lai duoc (het quota LLM/loi mang) - FE tu fallback. */
    public static RefineClarifyingAnswersResponse unavailable() {
        return new RefineClarifyingAnswersResponse(false, null);
    }
}
