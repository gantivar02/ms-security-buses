package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Repositories.UserRepository;
import com.AJJ.ms_security.Services.NegocioSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints administrativos para sincronizar manualmente con ms-negocio.
 * Util para migrar usuarios pre-existentes en Mongo que nunca pasaron
 * por el flujo de registro + sync automatico.
 *
 * Protegido por el mismo secret que el endpoint de ms-negocio
 * (header X-Sync-Secret). Asi nadie sin el secret puede disparar
 * un re-procesamiento masivo.
 */
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private NegocioSyncService negocioSyncService;

    @Value("${app.negocio.sync.secret}")
    private String syncSecret;

    /**
     * Itera todos los users de Mongo y los sincroniza con ms-negocio.
     * Devuelve un resumen con totales procesados.
     */
    @PostMapping("/all-users")
    public ResponseEntity<Map<String, Object>> syncAll(
            @RequestHeader(value = "X-Sync-Secret", required = false) String secret) {
        if (secret == null || !secret.equals(syncSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Secret invalido o no provisto"));
        }

        List<User> users = this.theUserRepository.findAll();
        int total = users.size();
        int procesados = 0;
        int fallidos = 0;

        for (User user : users) {
            try {
                this.negocioSyncService.syncUser(user);
                procesados++;
            } catch (Exception e) {
                fallidos++;
                System.err.println("[SyncAll] Fallo user "
                        + user.getEmail() + ": " + e.getMessage());
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("total_usuarios", total);
        body.put("procesados", procesados);
        body.put("fallidos", fallidos);
        return ResponseEntity.ok(body);
    }
}
