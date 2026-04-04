package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.Permission;
import com.AJJ.ms_security.Repositories.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository thePermissionRepository;

    public List<Permission> find() {
        return this.thePermissionRepository.findAll();
    }

    public Permission findById(String id) {
        return this.thePermissionRepository.findById(id).orElse(null);
    }

    public List<Permission> findByModule(String module) {
        return this.thePermissionRepository.findByModule(module);
    }

    public Permission create(Permission newPermission) {
        validatePermission(newPermission, null);
        return this.thePermissionRepository.save(newPermission);
    }

    public Permission update(String id, Permission newPermission) {
        Permission actualPermission = this.thePermissionRepository.findById(id).orElse(null);

        if (actualPermission == null) {
            return null;
        }

        validatePermission(newPermission, id);

        actualPermission.setName(newPermission.getName());
        actualPermission.setModule(newPermission.getModule());
        actualPermission.setAction(newPermission.getAction());
        actualPermission.setDescription(newPermission.getDescription());
        actualPermission.setUrl(newPermission.getUrl());
        actualPermission.setMethod(newPermission.getMethod());

        return this.thePermissionRepository.save(actualPermission);
    }

    public void delete(String id) {
        Permission thePermission = this.thePermissionRepository.findById(id).orElse(null);
        if (thePermission != null) {
            this.thePermissionRepository.delete(thePermission);
        }
    }

    private void validatePermission(Permission permission, String currentId) {
        if (permission.getName() == null || permission.getName().trim().isEmpty()) {
            throw new RuntimeException("Permission name is required");
        }

        if (permission.getModule() == null || permission.getModule().trim().isEmpty()) {
            throw new RuntimeException("Permission module is required");
        }

        if (permission.getAction() == null || permission.getAction().trim().isEmpty()) {
            throw new RuntimeException("Permission action is required");
        }

        if (permission.getUrl() == null || permission.getUrl().trim().isEmpty()) {
            throw new RuntimeException("Permission URL is required");
        }

        if (permission.getMethod() == null || permission.getMethod().trim().isEmpty()) {
            throw new RuntimeException("Permission method is required");
        }

        permission.setName(permission.getName().trim().toLowerCase());
        permission.setModule(permission.getModule().trim().toLowerCase());
        permission.setAction(permission.getAction().trim().toLowerCase());
        permission.setMethod(permission.getMethod().trim().toUpperCase());

        Permission byName = this.thePermissionRepository.findByName(permission.getName());
        if (byName != null && !byName.getId().equals(currentId)) {
            throw new RuntimeException("A permission with that name already exists");
        }

        Permission byUrlMethod = this.thePermissionRepository.findByUrlAndMethod(permission.getUrl(), permission.getMethod());
        if (byUrlMethod != null && !byUrlMethod.getId().equals(currentId)) {
            throw new RuntimeException("A permission with that URL and method already exists");
        }
    }
}