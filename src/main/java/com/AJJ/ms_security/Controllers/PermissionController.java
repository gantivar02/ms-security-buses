package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Models.Permission;
import com.AJJ.ms_security.Services.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    @Autowired
    private PermissionService thePermissionService;

    @GetMapping("")
    public List<Permission> find() {
        return this.thePermissionService.find();
    }

    @GetMapping("{id}")
    public Permission findById(@PathVariable String id) {
        return this.thePermissionService.findById(id);
    }

    @GetMapping("/module/{module}")
    public List<Permission> findByModule(@PathVariable String module) {
        return this.thePermissionService.findByModule(module);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Permission newPermission) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(this.thePermissionService.create(newPermission));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Permission newPermission) {
        try {
            Permission updated = this.thePermissionService.update(id, newPermission);
            if (updated == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Permission not found"));
            }
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable String id) {
        this.thePermissionService.delete(id);
    }
}