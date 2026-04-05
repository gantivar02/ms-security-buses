package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.Session;
import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Models.UserRole;
import com.AJJ.ms_security.Repositories.SessionRepository;
import com.AJJ.ms_security.Repositories.UserRepository;
import com.AJJ.ms_security.Repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class SecurityService {

    @Autowired
    private UserRepository theUserRepository;
    @Autowired
    private EncryptionService theEncryptionService;
    @Autowired
    private JwtService theJwtService;
    @Autowired
    private UserRoleRepository theUserRoleRepository;
    @Autowired
    private SessionRepository theSessionRepository;

    private static final int MAX_ATTEMPTS = 3;
    private static final long CODE_EXPIRATION_MS = 10 * 60 * 1000; // 10 minutos

    // HU-012: login ahora inicia el flujo 2FA en vez de retornar JWT directamente
    public Map<String, Object> login(User theNewUser) {
        User theActualUser = this.theUserRepository.getUserByEmail(theNewUser.getEmail());

        if (theActualUser == null ||
                !theActualUser.getPassword().equals(theEncryptionService.convertSHA256(theNewUser.getPassword()))) {
            return null;
        }

        // Invalida sesión parcial anterior si existía
        Session existing = this.theSessionRepository.findActiveSessionByUserId(theActualUser.getId());
        if (existing != null) {
            this.theSessionRepository.delete(existing);
        }

        // Genera código de 6 dígitos aleatorio
        String code2FA = String.format("%06d", new Random().nextInt(999999));
        Date expiration = new Date(System.currentTimeMillis() + CODE_EXPIRATION_MS);

        // Crea la sesión parcial (sin JWT todavía)
        Session theSession = new Session(code2FA, expiration, theActualUser);
        this.theSessionRepository.save(theSession);

        // Envía el código al email del usuario
        this.send2FAEmail(theActualUser.getEmail(), theActualUser.getName(), code2FA, false);

        // Retorna sessionId y email enmascarado para el frontend
        Map<String, Object> theResponse = new HashMap<>();
        theResponse.put("sessionId", theSession.getId());
        theResponse.put("maskedEmail", maskEmail(theActualUser.getEmail()));
        theResponse.put("expiration", expiration.getTime());
        theResponse.put("message", "Código de verificación enviado a su email");
        return theResponse;
    }

    // HU-012: verifica el código 2FA y retorna el JWT si es correcto
    public Map<String, Object> verify2FA(String sessionId, String code) {
        Session theSession = this.theSessionRepository.findById(sessionId).orElse(null);

        if (theSession == null) {
            return Map.of("error", "SESSION_INVALID");
        }

        // Sesión expirada
        if (theSession.getExpiration().before(new Date())) {
            this.theSessionRepository.delete(theSession);
            return Map.of("error", "SESSION_EXPIRED");
        }

        // Código incorrecto
        if (!theSession.getCode2FA().equals(code)) {
            theSession.setAttempts(theSession.getAttempts() + 1);
            int remaining = MAX_ATTEMPTS - theSession.getAttempts();

            // HU-012: después de 3 intentos fallidos invalida la sesión
            if (theSession.getAttempts() >= MAX_ATTEMPTS) {
                this.theSessionRepository.delete(theSession);
                return Map.of("error", "SESSION_BLOCKED");
            }

            this.theSessionRepository.save(theSession);
            return Map.of("error", "CODE_INVALID", "remainingAttempts", remaining);
        }

        // Código correcto — genera el JWT real
        User theUser = theSession.getUser();
        List<UserRole> userRoles = this.theUserRoleRepository.getRolesByUser(theUser.getId());
        List<String> roleNames = userRoles.stream()
                .filter(ur -> ur.getRole() != null)
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toList());

        String jwt = this.theJwtService.generateToken(theUser, roleNames);

        // Guarda el JWT en la sesión y la marca como completa
        theSession.setToken(jwt);
        theSession.setCode2FA(null);
        this.theSessionRepository.save(theSession);

        return Map.of("token", jwt);
    }

    // HU-012: reenvía un nuevo código 2FA
    public Map<String, Object> resend2FA(String sessionId) {
        Session theSession = this.theSessionRepository.findById(sessionId).orElse(null);

        if (theSession == null || theSession.getToken() != null) {
            return Map.of("error", "SESSION_INVALID");
        }

        // Genera nuevo código y resetea intentos
        String newCode = String.format("%06d", new Random().nextInt(999999));
        Date newExpiration = new Date(System.currentTimeMillis() + CODE_EXPIRATION_MS);

        theSession.setCode2FA(newCode);
        theSession.setExpiration(newExpiration);
        theSession.setAttempts(0);
        this.theSessionRepository.save(theSession);

        this.send2FAEmail(theSession.getUser().getEmail(), theSession.getUser().getName(), newCode, true);

        return Map.of(
                "message", "Nuevo código enviado",
                "expiration", newExpiration.getTime()
        );
    }

    // HU-012: enmascara el email → juanmagudelolopez@gmail.com → ju***@gmail.com
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    private void send2FAEmail(String email, String name, String code, boolean isResend) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String codeLabel = isResend ? "Tu nuevo código de verificación es" : "Tu código de verificación es";

            String body = "{"
                    + "\"to\":\"" + email + "\","
                    + "\"subject\":\"Código de verificación 2FA\","
                    + "\"message\":\"Hola " + name + ", " + codeLabel + ": "
                    + code + ". Válido por 10 minutos. No lo compartas con nadie.\""
                    + "}";

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject("http://localhost:5000/send-email", request, String.class);
        } catch (Exception e) {
            System.out.println("Error enviando código 2FA: " + e.getMessage());
        }
    }
    /*
    public boolean permissionsValidation(final HttpServletRequest request,
                                         @RequestBody Permission thePermission) {
        boolean success=this.theValidatorsService.validationRolePermission(request,thePermission.getUrl(),thePermission.getMethod());
        return success;
    }
    */

}

