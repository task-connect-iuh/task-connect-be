package vn.taskconnect.user.dto.response;

import java.util.UUID;
import vn.taskconnect.user.entity.ServiceCategory;

/**
 * Danh muc nhom dich vu tra ve cho client (GET /users/service-categories) - dung de FE
 * hien danh sach chon luc dang cong viec hoac khai bao ky nang Tasker.
 */
public record ServiceCategoryResponse(
        UUID id,
        String code,
        String name,
        String description,
        int minExperienceYears
) {

    /** Chuyen entity sang DTO tra ve qua API, tranh entity JPA lo ra ngoai lop controller. */
    public static ServiceCategoryResponse from(ServiceCategory category) {
        return new ServiceCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.getMinExperienceYears());
    }
}
