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

    public boolean addRolePermission(String roleId,
                                     String permissionId){

        Role role = this.theRoleRepository.findById(roleId).orElse(null);
        Permission permission = this.thePermissionRepository.findById(permissionId).orElse(null);

        if (role != null && permission != null){
            RolePermission theRolePermission = new RolePermission(permission,role);
            this.theRolePermissionRepository.save(theRolePermission);
            return true;
        }else{
            return false;
        }
    }

    public boolean removeRolePermission(String rolePermissionId){

        RolePermission rolePermission = this.theRolePermissionRepository.findById(rolePermissionId).orElse(null);

        if (rolePermission != null){
            this.theRolePermissionRepository.delete(rolePermission);
            return true;
        }else{
            return false;
        }
    }
    public List<RolePermission> getPermissionsByRole(String roleId) {
        return this.theRolePermissionRepository.getPermissionsByRole(roleId);
    }
}