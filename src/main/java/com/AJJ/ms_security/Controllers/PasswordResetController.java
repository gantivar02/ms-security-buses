package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Services.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/security")
public class PasswordResetController {

    @Autowired
    private PasswordResetService thePasswordResetService;

    // HU-013: endpoint público, solicita recuperación ingresando solo el email
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El email es requerido"));
        }

        String message = this.thePasswordResetService.requestPasswordReset(email);
        return ResponseEntity.ok(Map.of("message", message));
    }

    // HU-013: endpoint para confirmar el nuevo password con el token recibido por email
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
}
