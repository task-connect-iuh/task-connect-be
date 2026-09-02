package vn.taskconnect.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.taskconnect.user.dto.response.CategoryCertificateRequirementResponse;
import vn.taskconnect.user.entity.CategoryCertificateRequirement;
import vn.taskconnect.user.entity.CertificateType;
import vn.taskconnect.user.repository.CategoryCertificateRequirementRepository;
import vn.taskconnect.user.repository.CertificateTypeRepository;

/**
 * Unit test thuan tuy (khong DB) cho CertificateRequirementService. Ca hai entity chi co
 * constructor rong (protected, danh cho JPA) vi la bang Master Data - dung reflection de
 * dung field test, cung mau da dung o ServiceCategoryServiceTest (Buoc 2).
 */
class CertificateRequirementServiceTest {

    private final CertificateTypeRepository certificateTypeRepository = mock(CertificateTypeRepository.class);
    private final CategoryCertificateRequirementRepository requirementRepository =
            mock(CategoryCertificateRequirementRepository.class);
    private final CertificateRequirementService service =
            new CertificateRequirementService(certificateTypeRepository, requirementRepository);

    private static CertificateType certificateTypeOf(UUID id, String code, String name) {
        try {
            Constructor<CertificateType> constructor = CertificateType.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            CertificateType certificateType = constructor.newInstance();
            setField(CertificateType.class, certificateType, "id", id);
            setField(CertificateType.class, certificateType, "code", code);
            setField(CertificateType.class, certificateType, "name", name);
            setField(CertificateType.class, certificateType, "active", true);
            return certificateType;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CategoryCertificateRequirement requirementOf(UUID id, UUID categoryId, UUID certificateTypeId,
            boolean mandatory, int minExperienceYears) {
        try {
            Constructor<CategoryCertificateRequirement> constructor =
                    CategoryCertificateRequirement.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            CategoryCertificateRequirement requirement = constructor.newInstance();
            setField(CategoryCertificateRequirement.class, requirement, "id", id);
            setField(CategoryCertificateRequirement.class, requirement, "categoryId", categoryId);
            setField(CategoryCertificateRequirement.class, requirement, "certificateTypeId", certificateTypeId);
            setField(CategoryCertificateRequirement.class, requirement, "mandatory", mandatory);
            setField(CategoryCertificateRequirement.class, requirement, "minExperienceYears", minExperienceYears);
            return requirement;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Class<?> type, Object target, String fieldName, Object value)
            throws ReflectiveOperationException {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void should_returnActiveCertificateTypes_inRepositoryOrder() {
        CertificateType soCap = certificateTypeOf(UUID.randomUUID(), "SO_CAP_DIEN_DD", "Chứng chỉ sơ cấp Điện dân dụng");
        when(certificateTypeRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(soCap));

        List<CertificateType> result = service.listActiveCertificateTypes();

        assertThat(result).containsExactly(soCap);
    }

    // Quan he OR: nhieu dong mandatory=true cho cung mot category deu duoc tra ve, khong loc bot.
    @Test
    void should_joinCertificateTypeDetails_when_categoryHasMultipleAlternativeCertificates() {
        UUID categoryId = UUID.randomUUID();
        UUID soCapId = UUID.randomUUID();
        UUID trungCapId = UUID.randomUUID();
        CertificateType soCap = certificateTypeOf(soCapId, "SO_CAP_DIEN_DD", "Chứng chỉ sơ cấp Điện dân dụng");
        CertificateType trungCap = certificateTypeOf(trungCapId, "TRUNG_CAP_DIEN_DD", "Bằng trung cấp Điện dân dụng");
        CategoryCertificateRequirement req1 = requirementOf(UUID.randomUUID(), categoryId, soCapId, true, 0);
        CategoryCertificateRequirement req2 = requirementOf(UUID.randomUUID(), categoryId, trungCapId, true, 0);
        when(requirementRepository.findByCategoryId(categoryId)).thenReturn(List.of(req1, req2));
        when(certificateTypeRepository.findAllById(List.of(soCapId, trungCapId)))
                .thenReturn(List.of(soCap, trungCap));

        List<CategoryCertificateRequirementResponse> result = service.listRequirementsForCategory(categoryId);

        assertThat(result).containsExactly(
                new CategoryCertificateRequirementResponse(soCapId, "SO_CAP_DIEN_DD",
                        "Chứng chỉ sơ cấp Điện dân dụng", true, 0),
                new CategoryCertificateRequirementResponse(trungCapId, "TRUNG_CAP_DIEN_DD",
                        "Bằng trung cấp Điện dân dụng", true, 0));
    }

    @Test
    void should_returnEmptyList_when_categoryHasNoRequirements() {
        UUID categoryId = UUID.randomUUID();
        when(requirementRepository.findByCategoryId(categoryId)).thenReturn(List.of());
        when(certificateTypeRepository.findAllById(List.of())).thenReturn(List.of());

        assertThat(service.listRequirementsForCategory(categoryId)).isEmpty();
    }
}
