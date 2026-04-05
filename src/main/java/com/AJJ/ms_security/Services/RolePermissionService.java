package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.Permission;
import com.AJJ.ms_security.Models.Role;
import com.AJJ.ms_security.Models.RolePermission;
import com.AJJ.ms_security.Repositories.PermissionRepository;
import com.AJJ.ms_security.Repositories.RolePermissionRepository;
import com.AJJ.ms_security.Repositories.RoleRepository;
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

    public List<RolePermission> getPermissionsByRole(String roleId) {
        return this.theRolePermissionRepository.getPermissionsByRole(roleId);
    }

    public boolean addRolePermission(String roleId, String permissionId) {
        Role role = this.theRoleRepository.findById(roleId).orElse(null);
        Permission permission = this.thePermissionRepository.findById(permissionId).orElse(null);

        if (role == null || permission == null) {
            return false;
        }

        RolePermission existing = this.theRolePermissionRepository.getRolePermission(roleId, permissionId);
        if (existing != null) {
            throw new RuntimeException("This permission is already assigned to the role");
        }

        RolePermission theRolePermission = new RolePermission(permission, role);
        this.theRolePermissionRepository.save(theRolePermission);
        return true;
    }

    public boolean removeRolePermission(String rolePermissionId) {
        RolePermission rolePermission = this.theRolePermissionRepository.findById(rolePermissionId).orElse(null);

        if (rolePermission == null) {
            return false;
        }

        this.theRolePermissionRepository.delete(rolePermission);
        return true;
    }
    public List<RolePermission> getPermissionsByRole(String roleId) {
        return this.theRolePermissionRepository.getPermissionsByRole(roleId);
    }
}