package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Models.UserRole;
import com.AJJ.ms_security.Repositories.UserRepository;
import com.AJJ.ms_security.Repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sincroniza usuarios y roles de Mongo (ms-security) hacia la tabla `personas`
 * (y `ciudadanos`/`conductores`) en MySQL (ms-negocio).
 *
 * Llama a POST {app.negocio.url}/api/auth-sync/persona con el secret compartido.
 * Si la llamada falla, se loggea pero NO se propaga el error: el flujo
 * principal (registro de usuario, asignacion de rol) sigue funcionando.
 */
@Service
public class NegocioSyncService {

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private UserRepository theUserRepository;

    @Value("${app.negocio.url}")
    private String negocioUrl;

    @Value("${app.negocio.sync.secret}")
    private String syncSecret;

    /**
     * Sincroniza al user dado con ms-negocio. Resuelve los roles desde
     * la coleccion user_role (todos los roles asignados actualmente).
     */
    public void syncUser(User user) {
        if (user == null || user.getEmail() == null) {
            return;
        }
        try {
            List<UserRole> userRoles = this.theUserRoleRepository.getRolesByUser(user.getId());
            List<String> roleNames = userRoles.stream()
                    .map(ur -> ur.getRole() != null ? ur.getRole().getName() : null)
                    .filter(n -> n != null)
                    .collect(Collectors.toList());

            String fullName = buildFullName(user);

            Map<String, Object> payload = new HashMap<>();
            payload.put("email", user.getEmail());
            payload.put("name", fullName);
            payload.put("roles", roleNames);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Sync-Secret", syncSecret);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForObject(
                    negocioUrl + "/api/auth-sync/persona",
                    request,
                    String.class
            );

            System.out.println("[NegocioSync] OK email=" + user.getEmail()
                    + " roles=" + roleNames);
        } catch (Exception e) {
            System.err.println("[NegocioSync] FALLO sync email="
                    + user.getEmail() + " error=" + e.getMessage());
        }
    }

    /**
     * Variante por id (busca el user y delega en syncUser).
     */
    public void syncUserById(String userId) {
        if (userId == null) return;
        User user = this.theUserRepository.findById(userId).orElse(null);
        if (user != null) {
            this.syncUser(user);
        }
    }

    private String buildFullName(User user) {
        String name = user.getName() != null ? user.getName().trim() : "";
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
        if (name.isEmpty() && lastName.isEmpty()) {
            return user.getEmail();
        }
        if (name.isEmpty()) return lastName;
        if (lastName.isEmpty()) return name;
        return name + " " + lastName;
    }
}
