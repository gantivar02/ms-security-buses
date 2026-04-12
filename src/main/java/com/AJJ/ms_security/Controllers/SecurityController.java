package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Models.RegisterRequest;
import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Services.SecurityService;
import com.AJJ.ms_security.Services.UserService;
import jakarta.validation.Valid;
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
    @Autowired
    private UserService theUserService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, String> response = this.theUserService.registerPublicUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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

    // login con Google
    @PostMapping("/login-google")
    public ResponseEntity<Map<String, Object>> loginGoogle(@RequestBody Map<String, String> body) {

        String googleToken = body.get("token");

        if (googleToken == null || googleToken.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Token de Google es requerido"));
        }

        Map<String, Object> theResponse = this.theSecurityService.loginGoogle(googleToken);

        if (theResponse == null || theResponse.containsKey("error")) {
            String error = theResponse != null ? (String) theResponse.get("error") : null;

            if ("GOOGLE_ACCOUNT_MISMATCH".equals(error)) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "El email ya está vinculado a otra cuenta de Google"));
            }

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Autenticación con Google fallida"));
        }

        return ResponseEntity.ok(theResponse);
    }

    @PostMapping("/google/complete-profile")
    public ResponseEntity<Map<String, Object>> completeGoogleProfile(@RequestBody Map<String, String> body) {
        String onboardingToken = body.get("onboardingToken");
        String address = body.get("address");
        String phone = body.get("phone");

        if (onboardingToken == null || onboardingToken.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "El token de onboarding es requerido"));
        }

        Map<String, Object> theResponse = this.theSecurityService.completeGoogleProfile(onboardingToken, address, phone);
        if (!theResponse.containsKey("error")) {
            return ResponseEntity.ok(theResponse);
        }

        String error = (String) theResponse.get("error");
        return switch (error) {
            case "ADDRESS_REQUIRED" -> ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "La dirección es obligatoria para el registro como ciudadano"));
            case "USER_NOT_FOUND" -> ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado"));
            default -> ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "El onboarding de Google es inválido o expiró"));
        };
    }

    // HU-006: login con GitHub — el frontend envía el authorization code de GitHub
    @PostMapping("/login-github")
    public ResponseEntity<Map<String, Object>> loginGithub(@RequestBody Map<String, String> body) {

        String code = body.get("code");

        if (code == null || code.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Código de autorización de GitHub es requerido"));
        }

        Map<String, Object> theResponse = this.theSecurityService.loginGithub(code);

        if (theResponse.containsKey("error")) {
            String error = (String) theResponse.get("error");

            if ("GITHUB_EMAIL_REQUIRED".equals(error)) {
                // El usuario tiene email privado en GitHub — el frontend debe pedirlo
                return ResponseEntity
                        .status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of(
                                "message", "Su cuenta de GitHub tiene el email privado. Por favor proporcione un email alternativo",
                                "error", error
                        ));
            }

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Autenticación con GitHub fallida"));
        }

        return ResponseEntity.ok(theResponse);
    }

    // login microsoft
    @PostMapping("/login-microsoft")
    public ResponseEntity<Map<String, Object>> loginMicrosoft(@RequestBody Map<String, String> body) {

        String microsoftToken = body.get("token");

        if (microsoftToken == null || microsoftToken.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Token de Microsoft es requerido"));
        }

        Map<String, Object> theResponse = this.theSecurityService.loginMicrosoft(microsoftToken);

        if (theResponse == null || theResponse.containsKey("error")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Autenticación con Microsoft fallida"));
        }

        return ResponseEntity.ok(theResponse);
    }
}
