package vn.taskconnect.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * ErrorCode.code() phai luon trung ma HTTP that cua ErrorCode.status() - Javadoc cua
 * enum hua co unit test cho rang buoc nay, day la test do.
 */
class ErrorCodeTest {

    @Test
    void should_matchHttpStatusInCode_when_iteratingAllErrorCodes() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            String[] parts = errorCode.code().split("-");
            assertThat(parts)
                    .as("Ma loi %s phai co dang PREFIX-HTTPCODE-REASON", errorCode.code())
                    .hasSizeGreaterThanOrEqualTo(3);

            int numericPart = Integer.parseInt(parts[1]);
            assertThat(numericPart)
                    .as("Phan so trong ma %s phai trung HttpStatus that (%s)",
                            errorCode.code(), errorCode.status())
                    .isEqualTo(errorCode.status().value());
        }
    }
}
