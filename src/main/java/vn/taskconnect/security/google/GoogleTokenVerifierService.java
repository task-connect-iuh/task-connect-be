package vn.taskconnect.security.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.security.GoogleProperties;

/**
 * Verify ID token JWT do Google Identity Services phat (FE lay qua @react-oauth/google, xem
 * 16-api-contract.md - khong dung Authorization Code redirect flow). Chi can GOOGLE_CLIENT_ID
 * lam audience, khong can Client Secret vi khong trao doi authorization code voi Google.
 */
@Component
public class GoogleTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierService.class);

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(GoogleProperties googleProperties) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleProperties.clientId()))
                .build();
    }

    /**
     * Verify chu ky, audience (phai khop GOOGLE_CLIENT_ID) va han su dung (claim exp) cua
     * idToken. Khong dat TTL rieng cho buoc xac nhan lien ket tai khoan (AuthService.
     * confirmGoogleLink()) - vong doi tu bi gioi han boi chinh han cua ID token nay, thuong
     * ~1 gio, Google quy dinh, khong cau hinh duoc.
     *
     * @throws BusinessException {@link ErrorCode#INVALID_GOOGLE_TOKEN} neu token thieu, sai
     *                            dinh dang, sai chu ky, sai audience, hoac da het han.
     */
    public GoogleProfile verify(String idToken) {
        GoogleIdToken token;
        try {
            token = verifier.verify(idToken);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
            log.warn("Khong verify duoc Google ID token: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }
        if (token == null) {
            throw new BusinessException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        GoogleIdToken.Payload payload = token.getPayload();
        Boolean emailVerified = payload.getEmailVerified();
        // "name" khong co typed getter rieng nhu getSubject()/getEmail() - Payload la GenericJson
        // (Map<String,Object>), doc truc tiep qua get() giong cach getEmailVerified() noi bo doc "email_verified".
        String name = (String) payload.get("name");
        return new GoogleProfile(payload.getSubject(), payload.getEmail(), Boolean.TRUE.equals(emailVerified), name);
    }
}
