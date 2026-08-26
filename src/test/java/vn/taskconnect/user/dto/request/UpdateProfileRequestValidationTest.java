package vn.taskconnect.user.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test Bean Validation thuan tuy tren UpdateProfileRequest, khong can Spring context hay
 * DB - dung truc tiep jakarta.validation.Validator. Bao phu cac test case bien UC03-12,
 * 13, 14, 15 trong docs/QA-REPORT-USER-STEP1-PROFILE.md.
 */
class UpdateProfileRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static UpdateProfileRequest requestWithLocation(BigDecimal lat, BigDecimal lng) {
        return new UpdateProfileRequest("Nguyen Van A", null, null, "Quan 7", lat, lng);
    }

    // UC03-12: toa do dung bien hop le, khong duoc bao loi.
    @Test
    void should_haveNoViolations_when_coordinatesAreAtValidBoundary() {
        UpdateProfileRequest maxBoundary = requestWithLocation(new BigDecimal("90.0000000"), new BigDecimal("180.0000000"));
        UpdateProfileRequest minBoundary = requestWithLocation(new BigDecimal("-90.0000000"), new BigDecimal("-180.0000000"));

        assertThat(validator.validate(maxBoundary)).isEmpty();
        assertThat(validator.validate(minBoundary)).isEmpty();
    }

    // UC03-13: toa do vuot bien phai bi bao loi DecimalMax/DecimalMin.
    @Test
    void should_haveViolation_when_latitudeExceedsMax() {
        UpdateProfileRequest request = requestWithLocation(new BigDecimal("90.0000001"), null);

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertThat(violations).anySatisfy(violation ->
                assertThat(violation.getPropertyPath().toString()).isEqualTo("locationLat"));
    }

    @Test
    void should_haveViolation_when_longitudeBelowMin() {
        UpdateProfileRequest request = requestWithLocation(null, new BigDecimal("-180.0000001"));

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertThat(violations).anySatisfy(violation ->
                assertThat(violation.getPropertyPath().toString()).isEqualTo("locationLng"));
    }

    // UC03-14: do dai field dung toi da @Size, khong duoc bao loi.
    @Test
    void should_haveNoViolations_when_fieldsAreAtMaxAllowedLength() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "N".repeat(150), "a".repeat(500), "d".repeat(500), "o".repeat(255), null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    // UC03-15: do dai field vuot toi da 1 ky tu phai bi bao loi Size.
    @Test
    void should_haveViolation_when_fullNameExceedsMaxLengthByOne() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "N".repeat(151), null, null, "Quan 7", null, null);

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertThat(violations).anySatisfy(violation ->
                assertThat(violation.getPropertyPath().toString()).isEqualTo("fullName"));
    }

    @Test
    void should_haveViolation_when_operatingAreaExceedsMaxLengthByOne() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Nguyen Van A", null, null, "o".repeat(256), null, null);

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertThat(violations).anySatisfy(violation ->
                assertThat(violation.getPropertyPath().toString()).isEqualTo("operatingArea"));
    }
}
