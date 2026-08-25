package com.stuttgarttaxi.taxihub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
public class ResendEmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;

    public ResendEmailService(@Value("${resend.api-key}") String apiKey,
                               @Value("${resend.from-address}") String fromAddress) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.restClient = RestClient.builder().baseUrl(RESEND_API_URL).build();
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;">
                    <h2 style="color:#111;">Şifre Sıfırlama Talebi</h2>
                    <p>TaxiHub Stuttgart hesabınız için bir şifre sıfırlama talebinde bulunuldu.</p>
                    <p>
                        <a href="%s" style="display:inline-block;padding:12px 24px;background:#FFC107;color:#111;
                        text-decoration:none;border-radius:4px;font-weight:bold;">Şifremi Sıfırla</a>
                    </p>
                    <p>Bu link 30 dakika boyunca geçerlidir. Eğer bu talebi siz yapmadıysanız bu e-postayı yok sayabilirsiniz.</p>
                </div>
                """.formatted(resetLink);

        send(toEmail, "TaxiHub Stuttgart - Şifre Sıfırlama", html, "Reset linki: " + resetLink);
    }

    public void sendVerificationEmail(String toEmail, String code, long validityMinutes) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;">
                    <h2 style="color:#111;">Hesap Doğrulama</h2>
                    <p>TaxiHub Stuttgart hesabınızı doğrulamak için aşağıdaki kodu kullanın:</p>
                    <p style="font-size:28px;font-weight:bold;letter-spacing:4px;color:#111;
                       background:#FFC107;display:inline-block;padding:12px 24px;border-radius:6px;">%s</p>
                    <p>Bu kod %d dakika boyunca geçerlidir. Eğer bu talebi siz yapmadıysanız bu e-postayı yok sayabilirsiniz.</p>
                </div>
                """.formatted(code, validityMinutes);

        send(toEmail, "TaxiHub Stuttgart - Hesap Doğrulama Kodu", html, "Doğrulama kodu: " + code);
    }

    private void send(String toEmail, String subject, String html, String fallbackLogDetail) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY tanımlı değil; e-posta gönderilmedi. {}", fallbackLogDetail);
            return;
        }

        Map<String, Object> payload = Map.of(
                "from", fromAddress,
                "to", toEmail,
                "subject", subject,
                "html", html
        );

        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) ->
                            log.error("Resend API hatası: {} - {}", res.getStatusCode(), res.getBody()))
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error("E-posta gönderilemedi: {}", ex.getMessage(), ex);
        }
    }
}
