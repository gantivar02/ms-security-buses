package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.Session;
import com.AJJ.ms_security.Models.Profile;
import com.AJJ.ms_security.Models.Role;
import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Models.UserRole;
import com.AJJ.ms_security.Repositories.ProfileRepository;
import com.AJJ.ms_security.Repositories.RoleRepository;
import com.AJJ.ms_security.Repositories.SessionRepository;
import com.AJJ.ms_security.Repositories.UserRepository;
import com.AJJ.ms_security.Repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    @Autowired
    private ProfileRepository theProfileRepository;
    @Autowired
    private RoleRepository theRoleRepository;

    // HU-006: credenciales de la GitHub OAuth App
    @Value("${github.client-id}")
    private String githubClientId;

    @Value("${github.client-secret}")
    private String githubClientSecret;

    // URL del microservicio de notificaciones
    @Value("${app.email.service.url}")
    private String emailServiceUrl;

    private static final int MAX_ATTEMPTS = 3;
    private static final long CODE_EXPIRATION_MS = 10 * 60 * 1000; // 10 minutos

    // HU-012: login ahora inicia el flujo 2FA en vez de retornar JWT directamente
    public Map<String, Object> login(String email, String password) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        User theActualUser = this.theUserRepository.getUserByEmail(normalizedEmail);

        if (theActualUser == null ||
                !theActualUser.getPassword().equals(theEncryptionService.convertSHA256(password))) {
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

    // HU-012: cancela una sesión parcial cuando el usuario cierra la ventana antes de completar 2FA
    public void cancelPartialSession(String sessionId) {
        Session session = this.theSessionRepository.findById(sessionId).orElse(null);
        if (session != null && session.getToken() == null) {
            this.theSessionRepository.delete(session);
        }
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
            restTemplate.postForObject(emailServiceUrl + "/send-email", request, String.class);
        } catch (Exception e) {
            System.out.println("Error enviando código 2FA: " + e.getMessage());
        }
    }
    // HU-006: login con GitHub — recibe el authorization code del frontend
    public Map<String, Object> loginGithub(String code) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 1. Intercambiar el code por un access_token
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_JSON);
            tokenHeaders.set("Accept", "application/json");

            Map<String, String> tokenBody = new HashMap<>();
            tokenBody.put("client_id", githubClientId);
            tokenBody.put("client_secret", githubClientSecret);
            tokenBody.put("code", code);

            HttpEntity<Map<String, String>> tokenRequest = new HttpEntity<>(tokenBody, tokenHeaders);
            Map tokenResponse = restTemplate.postForObject(
                    "https://github.com/login/oauth/access_token",
                    tokenRequest,
                    Map.class
            );

            if (tokenResponse == null || tokenResponse.get("access_token") == null) {
                return Map.of("error", "GITHUB_AUTH_FAILED");
            }

            String accessToken = (String) tokenResponse.get("access_token");

            // 2. Obtener datos del usuario desde la API de GitHub
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.set("Authorization", "Bearer " + accessToken);
            userHeaders.set("Accept", "application/vnd.github+json");
            HttpEntity<?> userEntity = new HttpEntity<>(userHeaders);

            ResponseEntity<Map> userResponse = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    userEntity,
                    Map.class
            );

            Map userData = userResponse.getBody();
            if (userData == null) {
                return Map.of("error", "GITHUB_AUTH_FAILED");
            }

            String githubLogin = (String) userData.get("login");
            String name        = (String) userData.get("name");
            String email       = (String) userData.get("email");

            // 3. Si el email es privado en GitHub, consultar el endpoint de emails
            if (email == null || email.isBlank()) {
                email = this.getPrimaryGithubEmail(restTemplate, accessToken);
            }

            // Si sigue sin email, el frontend debe solicitar uno alternativo
            if (email == null || email.isBlank()) {
                return Map.of("error", "GITHUB_EMAIL_REQUIRED");
            }

            // Si el nombre público de GitHub está vacío, usar el username
            if (name == null || name.isBlank()) {
                name = githubLogin;
            }

            String normalizedEmail = email.toLowerCase().trim();

            // 4. Buscar usuario existente o crear uno nuevo
            User theUser = this.theUserRepository.getUserByEmail(normalizedEmail);
            if (theUser == null) {
                // Primera vez: crear cuenta automáticamente
                theUser = new User();
                theUser.setEmail(normalizedEmail);
                theUser.setName(name);
                theUser.setPassword("GITHUB_AUTH");
                theUser.setGithubUsername(githubLogin);
                this.theUserRepository.save(theUser);
            } else {
                // Cuenta existente: vincular o re-vincular GitHub
                if (theUser.getGithubUsername() == null) {
                    theUser.setGithubUsername(githubLogin);
                    this.theUserRepository.save(theUser);
                }
            }

            // 5. Obtener roles y generar JWT
            List<UserRole> userRoles = this.theUserRoleRepository.getRolesByUser(theUser.getId());
            List<String> roleNames = userRoles.stream()
                    .filter(ur -> ur.getRole() != null)
                    .map(ur -> ur.getRole().getName())
                    .collect(Collectors.toList());

            String jwt = this.theJwtService.generateToken(theUser, roleNames);
            return Map.of("token", jwt);

        } catch (Exception e) {
            return Map.of("error", "GITHUB_AUTH_FAILED");
        }
    }

    // HU-006: desvincula la cuenta de GitHub del usuario
    public Map<String, Object> unlinkGithubAccount(String userId) {
        User theUser = this.theUserRepository.findById(userId).orElse(null);
        if (theUser == null) {
            return Map.of("error", "USER_NOT_FOUND");
        }

        if (theUser.getGithubUsername() == null || theUser.getGithubUsername().isBlank()) {
            return Map.of("error", "GITHUB_NOT_LINKED");
        }

        theUser.setGithubUsername(null);
        this.theUserRepository.save(theUser);

        return Map.of(
                "message", "Cuenta de GitHub desvinculada correctamente."
        );
    }

    // HU-006: obtiene el email primario verificado cuando el usuario tiene email privado en GitHub
    private String getPrimaryGithubEmail(RestTemplate restTemplate, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/vnd.github+json");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            List<Map> emails = response.getBody();
            if (emails != null) {
                return emails.stream()
                        .filter(e -> Boolean.TRUE.equals(e.get("primary"))
                                  && Boolean.TRUE.equals(e.get("verified")))
                        .map(e -> (String) e.get("email"))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            // silencioso — el llamador maneja el null
        }
        return null;
    }

    /*
    public boolean permissionsValidation(final HttpServletRequest request,
                                         @RequestBody Permission thePermission) {
        boolean success=this.theValidatorsService.validationRolePermission(request,thePermission.getUrl(),thePermission.getMethod());
        return success;
    }
    */
    // HU-Google: login con access_token de Google (userinfo endpoint)
    public Map<String, Object> loginGoogle(String googleToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders googleHeaders = new HttpHeaders();
            googleHeaders.set("Authorization", "Bearer " + googleToken);
            HttpEntity<?> googleEntity = new HttpEntity<>(googleHeaders);

            ResponseEntity<Map> googleResponseEntity = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    googleEntity,
                    Map.class
            );
            Map googleResponse = googleResponseEntity.getBody();

            if (googleResponse == null || googleResponse.get("email") == null || googleResponse.get("sub") == null) {
                return Map.of("error", "TOKEN_INVALID");
            }

            String email = ((String) googleResponse.get("email")).toLowerCase().trim();
            String name = (String) googleResponse.get("name");
            String googleId = (String) googleResponse.get("sub");
            String picture = (String) googleResponse.get("picture");
            boolean emailVerified = Boolean.parseBoolean(String.valueOf(googleResponse.get("email_verified")));

            if (!emailVerified) {
                return Map.of("error", "TOKEN_INVALID");
            }

            User theUser = this.theUserRepository.getUserByEmail(email);
            boolean createdWithGoogle = false;

            if (theUser == null) {
                theUser = new User();
                theUser.setEmail(email);
                theUser.setName((name == null || name.isBlank()) ? email : name);
                theUser.setPassword("GOOGLE_AUTH");
                theUser.setGoogleId(googleId);
                this.theUserRepository.save(theUser);
                this.assignCitizenRole(theUser);
                createdWithGoogle = true;
            } else {
                if (theUser.getGoogleId() != null && !theUser.getGoogleId().equals(googleId)) {
                    return Map.of("error", "GOOGLE_ACCOUNT_MISMATCH");
                }

                // Vincular o re-vincular Google
                boolean shouldSaveUser = false;
                if (theUser.getGoogleId() == null) {
                    theUser.setGoogleId(googleId);
                    shouldSaveUser = true;
                }
                if ((theUser.getName() == null || theUser.getName().isBlank()) && name != null && !name.isBlank()) {
                    theUser.setName(name);
                    shouldSaveUser = true;
                }
                if (shouldSaveUser) {
                    this.theUserRepository.save(theUser);
                }
            }

            this.upsertGoogleProfile(theUser, picture);

            if (createdWithGoogle || this.requiresCitizenAddress(theUser)) {
                String onboardingToken = this.theJwtService.generateGoogleOnboardingToken(theUser);
                Map<String, Object> response = new HashMap<>();
                response.put("requiresProfileCompletion", true);
                response.put("provider", "google");
                response.put("onboardingToken", onboardingToken);
                response.put("userId", theUser.getId());
                response.put("email", theUser.getEmail());
                response.put("name", theUser.getName());
                response.put("message", "Debe completar su dirección para finalizar el registro como ciudadano");
                return response;
            }

            return Map.of("token", this.generateAccessToken(theUser));

        } catch (Exception e) {
            System.out.println("Error en loginGoogle: " + e.getMessage());
            return Map.of("error", "GOOGLE_AUTH_FAILED");
        }
    }

    public Map<String, Object> completeGoogleProfile(String onboardingToken, String address, String phone) {
        if (address == null || address.isBlank()) {
            return Map.of("error", "ADDRESS_REQUIRED");
        }

        User tokenUser = this.theJwtService.getUserFromGoogleOnboardingToken(onboardingToken);
        if (tokenUser == null) {
            return Map.of("error", "ONBOARDING_TOKEN_INVALID");
        }

        User theUser = this.theUserRepository.findById(tokenUser.getId()).orElse(null);
        if (theUser == null) {
            return Map.of("error", "USER_NOT_FOUND");
        }

        this.assignCitizenRole(theUser);

        Profile profile = this.theProfileRepository.findByUserId(theUser.getId());
        if (profile == null) {
            profile = new Profile();
            profile.setUser(theUser);
        }

        profile.setAddress(address.trim());
        if (phone != null && !phone.isBlank()) {
            profile.setPhone(phone.trim());
        }
        this.theProfileRepository.save(profile);

        return Map.of("token", this.generateAccessToken(theUser));
    }

    public Map<String, Object> unlinkGoogleAccount(String userId) {
        User theUser = this.theUserRepository.findById(userId).orElse(null);
        if (theUser == null) {
            return Map.of("error", "USER_NOT_FOUND");
        }

        if (theUser.getGoogleId() == null || theUser.getGoogleId().isBlank()) {
            return Map.of("error", "GOOGLE_NOT_LINKED");
        }

        theUser.setGoogleId(null);
        this.theUserRepository.save(theUser);

        return Map.of(
                "message", "Cuenta de Google desvinculada correctamente. Si desea iniciar sesión sin Google, utilice recuperación de contraseña para definir una contraseña local."
        );
    }

    private String generateAccessToken(User theUser) {
        List<UserRole> userRoles = this.theUserRoleRepository.getRolesByUser(theUser.getId());
        List<String> roleNames = userRoles.stream()
                .filter(ur -> ur.getRole() != null)
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toList());
        return this.theJwtService.generateToken(theUser, roleNames);
    }

    private void assignCitizenRole(User theUser) {
        Role citizenRole = this.theRoleRepository.findByNameIgnoreCase("Ciudadano");
        if (citizenRole == null) {
            return;
        }

        UserRole existingRole = this.theUserRoleRepository.findByUser_IdAndRole_Id(theUser.getId(), citizenRole.getId());
        if (existingRole == null) {
            this.theUserRoleRepository.save(new UserRole(theUser, citizenRole));
        }
    }

    private boolean requiresCitizenAddress(User theUser) {
        boolean isCitizen = this.theUserRoleRepository.getRolesByUser(theUser.getId()).stream()
                .map(UserRole::getRole)
                .filter(role -> role != null && role.getName() != null)
                .anyMatch(role -> "Ciudadano".equalsIgnoreCase(role.getName()));

        if (!isCitizen) {
            return false;
        }

        Profile profile = this.theProfileRepository.findByUserId(theUser.getId());
        return profile == null || profile.getAddress() == null || profile.getAddress().isBlank();
    }

    private void upsertGoogleProfile(User theUser, String picture) {
        if (picture == null || picture.isBlank()) {
            return;
        }

        Profile profile = this.theProfileRepository.findByUserId(theUser.getId());
        if (profile == null) {
            profile = new Profile();
            profile.setUser(theUser);
        }

        if (profile.getPhoto() == null || profile.getPhoto().isBlank()) {
            profile.setPhoto(picture);
            this.theProfileRepository.save(profile);
        }
    }

    // HU-Microsoft: login con token de Microsoft
    public Map<String, Object> loginMicrosoft(String microsoftToken) {
        try {
            // 1. Verificar el token con Microsoft
            RestTemplate restTemplate = new RestTemplate();
            String microsoftUrl = "https://graph.microsoft.com/v1.0/me";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + microsoftToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            Map microsoftResponse = restTemplate.exchange(
                    microsoftUrl,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            ).getBody();

            if (microsoftResponse == null || microsoftResponse.get("userPrincipalName") == null) {
                return Map.of("error", "TOKEN_INVALID");
            }

            String email = (String) microsoftResponse.get("userPrincipalName");
            String name  = (String) microsoftResponse.get("displayName");

            // 2. Buscar o crear usuario
            User theUser = this.theUserRepository.getUserByEmail(email);

            if (theUser == null) {
                theUser = new User();
                theUser.setEmail(email);
                theUser.setName(name);
                theUser.setPassword("MICROSOFT_AUTH");
                this.theUserRepository.save(theUser);
            }

            // 3. Generar JWT
            List<UserRole> userRoles = this.theUserRoleRepository.getRolesByUser(theUser.getId());
            List<String> roleNames = userRoles.stream()
                    .filter(ur -> ur.getRole() != null)
                    .map(ur -> ur.getRole().getName())
                    .collect(Collectors.toList());

            String jwt = this.theJwtService.generateToken(theUser, roleNames);

            return Map.of("token", jwt);

        } catch (Exception e) {
            System.out.println("Error en loginMicrosoft: " + e.getMessage());
            return Map.of("error", "MICROSOFT_AUTH_FAILED");
        }
    }

}

