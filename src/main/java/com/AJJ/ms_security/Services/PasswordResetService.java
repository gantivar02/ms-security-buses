package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.PasswordResetToken;
import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Repositories.PasswordResetTokenRepository;
import com.AJJ.ms_security.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private PasswordResetTokenRepository thePasswordResetTokenRepository;

    @Autowired
    private EncryptionService theEncryptionService;

    // HU-013: URL del frontend inyectada desde application.properties
    @Value("${app.frontend.url}")
    private String frontendUrl;

    // HU-013: mensaje genérico siempre, no revela si el email existe o no
    private static final String GENERIC_MESSAGE =
            "Si el email existe, recibirá instrucciones de recuperación";

    // HU-013: 30 minutos en milisegundos (criterio de aceptación)
    private static final long EXPIRATION_MS = 30 * 60 * 1000;

    public String requestPasswordReset(String email) {

        User theUser = this.theUserRepository.getUserByEmail(email);

        if (theUser != null) {

            // Si ya tiene un token activo, lo invalida antes de generar uno nuevo
            PasswordResetToken existing =
                    this.thePasswordResetTokenRepository.findActiveTokenByUserId(theUser.getId());
            if (existing != null) {
                existing.setUsed(true);
                this.thePasswordResetTokenRepository.save(existing);
            }

            // Genera token único con expiración de 30 minutos
            String token = UUID.randomUUID().toString();
            Date expiration = new Date(System.currentTimeMillis() + EXPIRATION_MS);

            PasswordResetToken theResetToken = new PasswordResetToken(token, expiration, theUser);
            this.thePasswordResetTokenRepository.save(theResetToken);

            // Envía el email con el enlace de recuperación
            this.sendRecoveryEmail(theUser.getEmail(), theUser.getName(), token);
        }

        // HU-013: siempre el mismo mensaje, sin importar si el email existe o no
        return GENERIC_MESSAGE;
    }

    public String resetPassword(String token, String newPassword) {

        PasswordResetToken theResetToken =
                this.thePasswordResetTokenRepository.findByToken(token);

        if (theResetToken == null) {
            return "TOKEN_INVALID";
        }

        // Valida que no esté expirado
        if (theResetToken.getExpiration().before(new Date())) {
            return "TOKEN_EXPIRED";
        }

        // Valida que no haya sido usado ya
        if (theResetToken.isUsed()) {
            return "TOKEN_ALREADY_USED";
        }

        // Actualiza la contraseña del usuario
        User theUser = theResetToken.getUser();
        theUser.setPassword(this.theEncryptionService.convertSHA256(newPassword));
        this.theUserRepository.save(theUser);

        // Marca el token como usado para que no pueda reutilizarse
        theResetToken.setUsed(true);
        this.thePasswordResetTokenRepository.save(theResetToken);

        return "SUCCESS";
    }

    private void sendRecoveryEmail(String email, String name, String token) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            String url = "http://localhost:5000/send-email";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String resetLink = frontendUrl + "/reset-password?token=" + token;

            String body = "{"
                    + "\"to\":\"" + email + "\","
                    + "\"subject\":\"Recuperación de contraseña\","
                    + "\"message\":\"Hola " + name + ", haz clic en el siguiente enlace para "
                    + "restablecer tu contraseña (válido por 30 minutos): " + resetLink + "\""
                    + "}";

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);

        } catch (Exception e) {
            System.out.println("Error enviando email de recuperación: " + e.getMessage());
        }
    }
}
