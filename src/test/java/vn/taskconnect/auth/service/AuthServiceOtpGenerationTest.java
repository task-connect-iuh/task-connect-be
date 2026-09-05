package vn.taskconnect.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.taskconnect.auth.repository.AuthAccountRepository;
import vn.taskconnect.auth.repository.AuthAccountRoleRepository;
import vn.taskconnect.auth.repository.AuthEmailChangeTokenRepository;
import vn.taskconnect.auth.repository.AuthEmailVerificationTokenRepository;
import vn.taskconnect.auth.repository.AuthPasswordResetTokenRepository;
import vn.taskconnect.auth.repository.AuthRefreshTokenRepository;
import vn.taskconnect.security.AdminProperties;
import vn.taskconnect.security.google.GoogleTokenVerifierService;
import vn.taskconnect.security.jwt.JwtProperties;
import vn.taskconnect.security.jwt.JwtTokenProvider;
import vn.taskconnect.user.api.UserFacade;

/**
 * generateOtp() khong dung bat ky dependency nao duoc inject qua constructor (chi dung
 * SecureRandom noi bo), nen mock toan bo dependency de dung mot AuthService that ma
 * khong cham DB - day la unit test thuan tuy, khong phai integration test.
 */
class AuthServiceOtpGenerationTest {

    private AuthService newService() {
        return new AuthService(
                mock(AuthAccountRepository.class),
                mock(AuthAccountRoleRepository.class),
                mock(AuthRefreshTokenRepository.class),
                mock(AuthEmailVerificationTokenRepository.class),
                mock(AuthPasswordResetTokenRepository.class),
                mock(AuthEmailChangeTokenRepository.class),
                mock(PasswordEncoder.class),
                mock(JwtTokenProvider.class),
                mock(GoogleTokenVerifierService.class),
                new JwtProperties("test-secret", 15, 15, false),
                new AdminProperties("admin@taskconnect.vn"),
                mock(UserFacade.class),
                mock(ApplicationEventPublisher.class),
                Clock.systemUTC());
    }

    @Test
    void should_returnSixAsciiDigits_when_generatingOtp() {
        AuthService service = newService();
        for (int i = 0; i < 10_000; i++) {
            assertThat(service.generateOtp()).matches("\\d{6}");
        }
    }

    @Test
    void should_coverEveryLeadingDigit_when_generatingManyOtps() {
        AuthService service = newService();
        Set<Character> leadingDigitsSeen = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            leadingDigitsSeen.add(service.generateOtp().charAt(0));
        }
        assertThat(leadingDigitsSeen)
                .as("Ca 10 chu so dau phai xuat hien - xac nhan zero-padding va khong lech phan bo")
                .hasSize(10);
    }
}
