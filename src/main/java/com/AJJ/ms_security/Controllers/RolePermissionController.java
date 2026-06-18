package com.AJJ.ms_security.Controllers;

import com.AJJ.ms_security.Models.RolePermission;
import com.AJJ.ms_security.Services.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role-permission")
public class RolePermissionController {

    @Autowired
    private RolePermissionService theRolePermissionService;

    /**
     * GET /role-permission/role/{roleId}
     * Obtiene todos los permisos asignados a un rol específico.
     * El frontend usa esto para mostrar qué permisos tiene cada rol
     * y así el admin puede ver el estado actual antes de modificar.
     */
    @GetMapping("/role/{roleId}")
    public List<RolePermission> getPermissionsByRole(@PathVariable String roleId) {
        return this.theRolePermissionService.getPermissionsByRole(roleId);
    }

    /**
     * POST /role-permission/{roleId}/permission/{permissionId}
     * Asigna un permiso a un rol.
     * Valida que no exista duplicado y que ambos existan.
     * Retorna 409 CONFLICT si ya estaba asignado.
     */
    @PostMapping("/{roleId}/permission/{permissionId}")
    public ResponseEntity<Map<String, String>> addRolePermission(
            @PathVariable String roleId,
            @PathVariable String permissionId) {

        boolean response = this.theRolePermissionService.addRolePermission(roleId, permissionId);

        if (response) {
            return ResponseEntity.ok(Map.of("message", "Permiso asignado al rol exitosamente"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "El rol o permiso no existe, o el permiso ya está asignado a este rol"));
        }
    }

    /**
     * DELETE /role-permission/{rolePermissionId}
     * Elimina una asignación por su ID directo.
     */
    @DeleteMapping("/{rolePermissionId}")
    public ResponseEntity<Map<String, String>> removeRolePermission(
            @PathVariable String rolePermissionId) {

        boolean response = this.theRolePermissionService.removeRolePermission(rolePermissionId);

        if (response) {
            return ResponseEntity.ok(Map.of("message", "Permiso revocado exitosamente"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "La asignación rol-permiso no fue encontrada"));
        }
    }

    /**
     * DELETE /role-permission/{roleId}/permission/{permissionId}
     * Revoca un permiso de un rol usando sus IDs directamente.
     * Más intuitivo: "quitar el permiso X del rol Y".
     */
    @DeleteMapping("/{roleId}/permission/{permissionId}")
    public ResponseEntity<Map<String, String>> removeByRoleAndPermission(
            @PathVariable String roleId,
            @PathVariable String permissionId) {

        boolean response = this.theRolePermissionService.removeByRoleAndPermission(roleId, permissionId);

        if (response) {
            return ResponseEntity.ok(Map.of("message", "Permiso revocado del rol exitosamente"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "El rol no tiene este permiso asignado"));
        }
    }
}