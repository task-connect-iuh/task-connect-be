package vn.taskconnect.user.dto.response;

import java.util.UUID;
import vn.taskconnect.user.entity.CertificateType;

/**
 * Loai chung chi tra ve cho client (GET /users/certificate-types) - dung de FE hien danh
 * sach chung chi khi Tasker khai bao ky nang (Buoc 6).
 */
public record CertificateTypeResponse(
        UUID id,
        String code,
        String name,
        String issuingAuthority,
        String description
) {

    /** Chuyen entity sang DTO tra ve qua API, tranh entity JPA lo ra ngoai lop controller. */
    public static CertificateTypeResponse from(CertificateType certificateType) {
        return new CertificateTypeResponse(
                certificateType.getId(),
                certificateType.getCode(),
                certificateType.getName(),
                certificateType.getIssuingAuthority(),
                certificateType.getDescription());
    }
}
