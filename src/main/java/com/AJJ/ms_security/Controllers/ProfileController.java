package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Models.Profile;
import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Services.ProfileService;
import com.AJJ.ms_security.Services.SecurityService;
import com.AJJ.ms_security.Services.ValidatorsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    @Autowired
    private ProfileService theProfileService;
    @Autowired
    private SecurityService theSecurityService;
    @Autowired
    private ValidatorsService theValidatorsService;

    @GetMapping("")
    public List<Profile> find() {
        return this.theProfileService.find();
    }

    @GetMapping("{id}")
    public Profile findById(@PathVariable String id) {
        return this.theProfileService.findById(id);
    }

    @PostMapping
    public Profile create(@RequestBody Profile newProfile) {
        return this.theProfileService.create(newProfile);
    }

    @PutMapping("{id}")
    public Profile update(@PathVariable String id, @RequestBody Profile newProfile) {
        return this.theProfileService.update(id, newProfile);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable String id) {
        this.theProfileService.delete(id);
    }

    @PutMapping("/google/unlink")
    public ResponseEntity<Map<String, String>> unlinkGoogle(HttpServletRequest request) {
        User currentUser = this.theValidatorsService.getUser(request);
        if (currentUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Sesión inválida"));
        }

        Map<String, Object> response = this.theSecurityService.unlinkGoogleAccount(currentUser.getId());
        if (!response.containsKey("error")) {
            return ResponseEntity.ok(Map.of("message", (String) response.get("message")));
        }

        String error = (String) response.get("error");
        return switch (error) {
            case "GOOGLE_NOT_LINKED" -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "La cuenta no tiene Google vinculado"));
            case "USER_NOT_FOUND" -> ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario no encontrado"));
            default -> ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No fue posible desvincular Google"));
        };
    }

}
