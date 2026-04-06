package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Services.SecurityService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/public/security")
public class SecurityController {

    @Autowired
    private SecurityService theSecurityService;

    // HU-012: login ahora inicia el flujo 2FA, ya no retorna JWT directamente
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody User theNewUser,
            final HttpServletResponse response) throws IOException {

        Map<String, Object> theResponse = this.theSecurityService.login(theNewUser);

        if (theResponse == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciales incorrectas"));
        }

        return ResponseEntity.ok(theResponse);
    }

    // HU-012: verifica el código de 6 dígitos y retorna el JWT si es correcto
    @PostMapping("/verify-2fa")
    public ResponseEntity<Map<String, Object>> verify2FA(@RequestBody Map<String, String> body) {

        String sessionId = body.get("sessionId");
        String code = body.get("code");

        if (sessionId == null || code == null || sessionId.isBlank() || code.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "sessionId y code son requeridos"));
        }

        Map<String, Object> theResponse = this.theSecurityService.verify2FA(sessionId, code);

        if (theResponse.containsKey("token")) {
            return ResponseEntity.ok(theResponse);
        }

        String error = (String) theResponse.get("error");

        return switch (error) {
            case "CODE_INVALID" -> ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Código incorrecto. Intentos restantes: " + theResponse.get("remainingAttempts"),
                            "remainingAttempts", theResponse.get("remainingAttempts")
                    ));
            case "SESSION_BLOCKED" -> ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Sesión bloqueada por demasiados intentos. Vuelva a iniciar sesión"));
            case "SESSION_EXPIRED" -> ResponseEntity
                    .status(HttpStatus.GONE)
                    .body(Map.of("message", "El código ha expirado. Vuelva a iniciar sesión"));
            default -> ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Sesión inválida"));
        };
    }

    // HU-012: reenvía un nuevo código 2FA al email del usuario
    @PostMapping("/resend-2fa")
    public ResponseEntity<Map<String, Object>> resend2FA(@RequestBody Map<String, String> body) {

        String sessionId = body.get("sessionId");

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "sessionId es requerido"));
        }

        Map<String, Object> theResponse = this.theSecurityService.resend2FA(sessionId);

        if (theResponse.containsKey("error")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Sesión inválida o ya completada"));
        }

        return ResponseEntity.ok(theResponse);
    }
}



