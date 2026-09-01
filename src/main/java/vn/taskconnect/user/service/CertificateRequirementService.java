package vn.taskconnect.user.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.user.dto.response.CategoryCertificateRequirementResponse;
import vn.taskconnect.user.entity.CategoryCertificateRequirement;
import vn.taskconnect.user.entity.CertificateType;
import vn.taskconnect.user.repository.CategoryCertificateRequirementRepository;
import vn.taskconnect.user.repository.CertificateTypeRepository;

/**
 * Nghiep vu doc loai chung chi va yeu cau chung chi theo category (Master Data, Buoc 5) -
 * chi doc qua API, sua bang migration. Chuan bi du lieu cho Buoc 6 (dang ky ky nang gop nop
 * chung chi) biet truoc category nao chap nhan chung chi gi.
 */
@Service
public class CertificateRequirementService {

    private final CertificateTypeRepository certificateTypeRepository;
    private final CategoryCertificateRequirementRepository requirementRepository;

    public CertificateRequirementService(CertificateTypeRepository certificateTypeRepository,
            CategoryCertificateRequirementRepository requirementRepository) {
        this.certificateTypeRepository = certificateTypeRepository;
        this.requirementRepository = requirementRepository;
    }

    /** Danh sach loai chung chi con hien hanh, sap theo ten, dung cho GET /users/certificate-types. */
    @Transactional(readOnly = true)
    public List<CertificateType> listActiveCertificateTypes() {
        return certificateTypeRepository.findByActiveTrueOrderByNameAsc();
    }

    /**
     * Danh sach chung chi duoc chap nhan cho mot category, kem ten/co quan cap - ghep tu
     * hai bang (khong co JPA relation giua hai entity, dung mau raw UUID nhat quan voi
     * phan con lai cua module User) bang mot lan doc hang loat CertificateType theo id,
     * tranh N+1 query.
     */
    @Transactional(readOnly = true)
    public List<CategoryCertificateRequirementResponse> listRequirementsForCategory(UUID categoryId) {
        List<CategoryCertificateRequirement> requirements = requirementRepository.findByCategoryId(categoryId);
        List<UUID> certificateTypeIds = requirements.stream()
                .map(CategoryCertificateRequirement::getCertificateTypeId)
                .toList();
        Map<UUID, CertificateType> certificateTypesById = certificateTypeRepository.findAllById(certificateTypeIds)
                .stream()
                .collect(Collectors.toMap(CertificateType::getId, Function.identity()));

        return requirements.stream()
                .map(requirement -> {
                    CertificateType certificateType = certificateTypesById.get(requirement.getCertificateTypeId());
                    return new CategoryCertificateRequirementResponse(
                            certificateType.getId(),
                            certificateType.getCode(),
                            certificateType.getName(),
                            requirement.isMandatory(),
                            requirement.getMinExperienceYears());
                })
                .toList();
    }
}
