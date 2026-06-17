package com.AJJ.ms_security.Controllers;


import com.AJJ.ms_security.Models.Role;
import com.AJJ.ms_security.Services.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService theRoleService;

    @GetMapping("")
    public List<Role> find() {
        return this.theRoleService.find();
    }

    @GetMapping("{id}")
    public Role findById(@PathVariable String id) {
        return this.theRoleService.findById(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Role newRole) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(this.theRoleService.create(newRole));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Role newRole) {
        try {
            Role updated = this.theRoleService.update(id, newRole);
            if (updated == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Role not found"));
            }
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        boolean result = this.theRoleService.delete(id);
        if (result) {
            return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Role not found or has users assigned"));
        }
    }

}
