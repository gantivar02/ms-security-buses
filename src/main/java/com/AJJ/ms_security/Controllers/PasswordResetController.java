package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Services.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/public/security")
public class PasswordResetController {

    @Autowired
    private PasswordResetService thePasswordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");
        String recaptchaToken = body.get("recaptchaToken");

        if (email == null || email.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El email es requerido"));
        }

        // Verificar reCAPTCHA
        if (!verifyRecaptcha(recaptchaToken)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Verificación reCAPTCHA fallida"));
        }

        String message = this.thePasswordResetService.requestPasswordReset(email);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody Map<String, String> body) {

        String token = body.get("token");
        String newPassword = body.get("newPassword");

        if (token == null || newPassword == null || token.isBlank() || newPassword.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Token y nueva contraseña son requeridos"));
        }

        String result = this.thePasswordResetService.resetPassword(token, newPassword);

        return switch (result) {
            case "SUCCESS" -> ResponseEntity.ok(
                    Map.of("message", "Contraseña actualizada exitosamente"));
            case "TOKEN_EXPIRED" -> ResponseEntity
                    .status(HttpStatus.GONE)
                    .body(Map.of("message", "El enlace de recuperación ha expirado"));
            case "TOKEN_ALREADY_USED" -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Este enlace ya fue utilizado"));
            default -> ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Token inválido"));
        };
    }

    private boolean verifyRecaptcha(String token) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://www.google.com/recaptcha/api/siteverify?secret=6LeHpbEsAAAAAJoZs3uyZGVLnSK5V8TTXBJomalS&response=" + token;
            Map response = restTemplate.postForObject(url, null, Map.class);
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            System.out.println("Error verificando reCAPTCHA: " + e.getMessage());
            return false;
        }
    }
}