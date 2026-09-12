package vn.taskconnect.security.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.security.FirebaseProperties;

/**
 * Verify ID token JWT do Firebase Phone Auth phat sau khi FE xac minh so dien thoai (test
 * whitelist hoac SMS that qua signInWithPhoneNumber, xem features/auth/PhoneVerificationFlow.tsx
 * ben FE) - dung Firebase Admin SDK, KHONG tin tuong ket qua client bao ve ma phai xac minh
 * chu ky/han su dung/audience server-side, cung tinh than voi GoogleTokenVerifierService.
 */
@Component
public class FirebaseTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenVerifierService.class);

    private final FirebaseProperties firebaseProperties;

    /**
     * Khong khoi tao FirebaseApp ngay trong constructor - service-account JSON co the chua
     * duoc cau hinh trong .env luc dev/test (vd chay unit test khac khong dung tinh nang
     * nay), khoi tao som se lam FAIL toan bo Spring context chi vi thieu bien moi truong
     * khong lien quan. Thay vao do khoi tao lazy, dung mot lan luc verify() dau tien.
     */
    public FirebaseTokenVerifierService(FirebaseProperties firebaseProperties) {
        this.firebaseProperties = firebaseProperties;
    }

    /**
     * Khoi tao FirebaseApp mot lan duy nhat, dung khi verify() dau tien duoc goi - guard
     * getApps().isEmpty() de an toan khi bi goi nhieu lan dong thoi hoac context reload,
     * FirebaseApp.initializeApp() nem loi neu goi lan 2 voi cung ten app mac dinh.
     */
    private synchronized void ensureInitialized() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        byte[] serviceAccountJson = Base64.getDecoder().decode(firebaseProperties.serviceAccountJsonBase64());
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountJson)))
                    .setProjectId(firebaseProperties.projectId())
                    .build();
            FirebaseApp.initializeApp(options);
        } catch (IOException ex) {
            throw new IllegalStateException("Khong doc duoc Firebase service-account JSON", ex);
        }
    }

    /**
     * Verify chu ky, audience (phai khop FIREBASE_PROJECT_ID) va han su dung cua idToken.
     *
     * @throws BusinessException {@link ErrorCode#INVALID_FIREBASE_TOKEN} neu token thieu,
     *                            sai dinh dang, sai chu ky, sai project, hoac da het han.
     */
    public FirebasePhoneProfile verify(String idToken) {
        ensureInitialized();
        FirebaseToken token;
        try {
            token = FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException | IllegalArgumentException ex) {
            log.warn("Khong verify duoc Firebase ID token: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.INVALID_FIREBASE_TOKEN);
        }

        // "phone_number" khong co typed getter rieng - Firebase Phone Auth dinh kem claim
        // nay truc tiep tren ID token, doc qua getClaims() (Map<String,Object>) giong cach
        // GoogleTokenVerifierService doc claim "name" tu GoogleIdToken.Payload.
        Object phoneNumber = token.getClaims().get("phone_number");
        if (!(phoneNumber instanceof String phone) || phone.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_FIREBASE_TOKEN);
        }
        return new FirebasePhoneProfile(token.getUid(), phone);
    }
}
