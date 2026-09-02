package vn.taskconnect.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.taskconnect.user.api.dto.ServiceCategorySummary;
import vn.taskconnect.user.entity.ServiceCategory;
import vn.taskconnect.user.repository.ServiceCategoryRepository;
import vn.taskconnect.user.repository.UserProfileRepository;

/**
 * Unit test thuan tuy (khong DB) cho ServiceCategoryService va phan danh muc cua
 * UserFacadeImpl. Entity ServiceCategory chi co constructor rong (protected, danh cho
 * JPA) vi bang Master Data khong tao/sua qua code - dung reflection de dung field test
 * thay vi them constructor chi phuc vu test.
 */
class ServiceCategoryServiceTest {

    private final ServiceCategoryRepository repository = mock(ServiceCategoryRepository.class);
    private final ServiceCategoryService service = new ServiceCategoryService(repository);

    private static ServiceCategory categoryOf(UUID id, String code, String name, int minExperienceYears) {
        try {
            Constructor<ServiceCategory> constructor = ServiceCategory.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            ServiceCategory category = constructor.newInstance();
            setField(category, "id", id);
            setField(category, "code", code);
            setField(category, "name", name);
            setField(category, "minExperienceYears", minExperienceYears);
            setField(category, "active", true);
            return category;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = ServiceCategory.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Danh sach tra ve dung nhu repository, sap theo ten (uy quyen sap xep cho query derived method).
    @Test
    void should_returnActiveCategories_inRepositoryOrder() {
        ServiceCategory dienDanDung = categoryOf(UUID.randomUUID(), "DIEN_DAN_DUNG", "Điện dân dụng", 0);
        ServiceCategory dienLanh = categoryOf(UUID.randomUUID(), "DIEN_LANH", "Điện lạnh", 0);
        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(dienDanDung, dienLanh));

        List<ServiceCategory> result = service.listActiveCategories();

        assertThat(result).containsExactly(dienDanDung, dienLanh);
    }

    // UserFacade.listActiveServiceCategories phai anh xa dung sang DTO cong khai cho module khac.
    @Test
    void should_mapToServiceCategorySummary_when_facadeListsActiveCategories() {
        UUID categoryId = UUID.randomUUID();
        ServiceCategory dienLanh = categoryOf(categoryId, "DIEN_LANH", "Điện lạnh", 2);
        UserProfileRepository profileRepository = mock(UserProfileRepository.class);
        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(dienLanh));
        UserFacadeImpl facade = new UserFacadeImpl(profileRepository, repository, Clock.systemUTC());

        List<ServiceCategorySummary> result = facade.listActiveServiceCategories();

        assertThat(result).containsExactly(new ServiceCategorySummary(categoryId, "DIEN_LANH", "Điện lạnh", 2));
    }
}
