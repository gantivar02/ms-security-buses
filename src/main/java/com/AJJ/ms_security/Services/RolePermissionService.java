package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.Role;
import com.AJJ.ms_security.Models.Permission;
import com.AJJ.ms_security.Models.RolePermission;
import com.AJJ.ms_security.Repositories.RoleRepository;
import com.AJJ.ms_security.Repositories.PermissionRepository;
import com.AJJ.ms_security.Repositories.RolePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolePermissionService {

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private PermissionRepository thePermissionRepository;

    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    /**
     * Obtiene todos los permisos asignados a un rol específico.
     * Permite al admin ver qué puede hacer cada rol en el sistema.
     */
    public List<RolePermission> getPermissionsByRole(String roleId) {
        return this.theRolePermissionRepository.getPermissionsByRole(roleId);
    }

    /**
     * Asigna un permiso a un rol.
     * Valida que tanto el rol como el permiso existan,
     * y que no se duplique la asignación.
     */
    public boolean addRolePermission(String roleId, String permissionId) {

        Role role = this.theRoleRepository.findById(roleId).orElse(null);
        Permission permission = this.thePermissionRepository.findById(permissionId).orElse(null);

        if (role == null || permission == null) {
            return false;
        }

        // Validar que no exista ya esta combinación rol-permiso
        RolePermission existing = this.theRolePermissionRepository.getRolePermission(roleId, permissionId);
        if (existing != null) {
            return false;
        }

        RolePermission theRolePermission = new RolePermission(permission, role);
        this.theRolePermissionRepository.save(theRolePermission);
        return true;
    }

    /**
     * Elimina una asignación rol-permiso por su ID directo.
     */
    public boolean removeRolePermission(String rolePermissionId) {

        RolePermission rolePermission = this.theRolePermissionRepository.findById(rolePermissionId).orElse(null);

        if (rolePermission != null) {
            this.theRolePermissionRepository.delete(rolePermission);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Elimina una asignación buscando por roleId + permissionId.
     * Más intuitivo para el admin: "quitar permiso X del rol Y".
     */
    public boolean removeByRoleAndPermission(String roleId, String permissionId) {

        RolePermission rolePermission = this.theRolePermissionRepository.getRolePermission(roleId, permissionId);

        if (rolePermission != null) {
            this.theRolePermissionRepository.delete(rolePermission);
            return true;
        } else {
            return false;
        }
    }
}