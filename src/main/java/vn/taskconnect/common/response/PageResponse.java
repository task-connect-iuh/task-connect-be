package vn.taskconnect.common.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Vo boc thong nhat cho moi phan hoi liet ke co phan trang, theo dung hop dong
 * {@code page/size} da chot trong .claude/rules/16-api-contract.md. Dat trong "data" cua
 * {@link ApiResponse} nhu binh thuong, khong phai mot tang boc rieng o ngoai.
 *
 * @param content danh sach ban ghi trang hien tai
 * @param page    so trang hien tai, bat dau tu 0
 * @param size    so ban ghi toi da mot trang
 * @param totalElements tong so ban ghi khop dieu kien loc, tren tat ca cac trang
 * @param totalPages tong so trang
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /** Chuyen Page<E> cua Spring Data sang PageResponse<T>, anh xa tung phan tu qua mapper. */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /** Chuyen Page<T> sang PageResponse<T> khi service da tra ve dung kieu DTO, khong can anh xa them. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return from(page, Function.identity());
    }
}
