package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Models.Session;
import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Models.UserRole;
import com.AJJ.ms_security.Repositories.UserRoleRepository;
import com.AJJ.ms_security.Services.JwtService;
import com.AJJ.ms_security.Services.SessionService;
import com.AJJ.ms_security.Services.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private SessionService theSessionService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRoleRepository theUserRoleRepository;
    @Autowired
    private JwtService jwtService;

    @GetMapping("")
    public List<Session> find() {
        return this.theSessionService.find();
    }

    @GetMapping("{id}")
    public Session findById(@PathVariable String id) {
        return this.theSessionService.findById(id);
    }

    @PostMapping
    public Session create(@RequestBody Session newSession) {
        return this.theSessionService.create(newSession);
    }

    @PutMapping("{id}")
    public Session update(@PathVariable String id, @RequestBody Session newSession) {
        return this.theSessionService.update(id, newSession);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable String id) {
        this.theSessionService.delete(id);
    }

    @PostMapping("/microsoft")
    public ResponseEntity<?> loginMicrosoft(@RequestBody Map<String, String> body) {

        String idToken = body.get("token");

        if (idToken == null) {
            return ResponseEntity.badRequest().body("Token no enviado");
        }

        try {
            // 👇 1. Decodificar token (sin validar aún)
            String[] parts = idToken.split("\\.");
            String payload = new String(Base64.getDecoder().decode(parts[1]));

            // 👇 2. Convertir a JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(payload);

            String email = json.get("preferred_username").asText();
            String name = json.get("name").asText();

            // 👇 3. Buscar usuario
            User user = userService.findByEmail(email);

            if (user == null) {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setPassword("MICROSOFT_LOGIN");
                user = userService.create(newUser);
            }

            // 👇 4. Generar JWT propio (incluye roles del usuario)
            List<String> roleNames = theUserRoleRepository.getRolesByUser(user.getId()).stream()
                    .filter(ur -> ur.getRole() != null)
                    .map(ur -> ur.getRole().getName())
                    .collect(Collectors.toList());
            String jwt = jwtService.generateToken(user, roleNames);

            // 👇 5. Respuesta
            return ResponseEntity.ok(Map.of("token", jwt));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error procesando token");
        }
    }
    @PostMapping("/google")
    public ResponseEntity<?> loginGoogle(@RequestBody Map<String, String> body) {

        String idToken = body.get("token");

        if (idToken == null) {
            return ResponseEntity.badRequest().body("Token no enviado");
        }

        try {
            String[] parts = idToken.split("\\.");
            String payload = new String(Base64.getDecoder().decode(parts[1]));

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(payload);

            String email = json.get("email").asText();
            String name = json.get("name").asText();

            User user = userService.findByEmail(email);

            if (user == null) {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setPassword("GOOGLE_LOGIN");
                user = userService.create(newUser);
            }

            List<String> roleNames = theUserRoleRepository.getRolesByUser(user.getId()).stream()
                    .filter(ur -> ur.getRole() != null)
                    .map(ur -> ur.getRole().getName())
                    .collect(Collectors.toList());
            String jwt = jwtService.generateToken(user, roleNames);

            return ResponseEntity.ok(Map.of("token", jwt));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error procesando token");
        }
    }

}