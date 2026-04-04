package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Models.RolePermission;
import com.AJJ.ms_security.Services.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/role-permission")
public class RolePermissionController {

    @Autowired
    private RolePermissionService theRolePermissionService;

    @GetMapping("/{roleId}/permissions")
    public List<RolePermission> getPermissionsByRole(@PathVariable String roleId) {
        return this.theRolePermissionService.getPermissionsByRole(roleId);
    }

    @PostMapping("/{roleId}/permission/{permissionId}")
    public ResponseEntity<Map<String, String>> addRolePermission(
            @PathVariable String roleId,
            @PathVariable String permissionId) {

        try {
            boolean response = this.theRolePermissionService.addRolePermission(roleId, permissionId);

            if (response) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("message", "Permission assigned successfully"));
            } else {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Role or Permission not found"));
            }
        } catch (RuntimeException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{rolePermissionId}")
    public ResponseEntity<Map<String, String>> removeRolePermission(
            @PathVariable String rolePermissionId) {

        boolean response = this.theRolePermissionService.removeRolePermission(rolePermissionId);

        if (response) {
            return ResponseEntity.ok(Map.of("message", "Permission removed successfully"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "RolePermission not found"));
        }
    }
}