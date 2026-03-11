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


    public Permission create(Permission newPermission) {
        return this.thePermissionRepository.save(newPermission);
    }


    public Permission update(String id, Permission newPermission) {
        Permission actualPermission = this.thePermissionRepository.findById(id).orElse(null);

        if (actualPermission != null) {
            actualPermission.setUrl(newPermission.getUrl());
            actualPermission.setMethod(newPermission.getMethod());
            actualPermission.setModel(newPermission.getModel());
            this.thePermissionRepository.save(actualPermission);
            return actualPermission;
        } else {
            return null;
        }
    }

    public void delete(String id) {
        Permission thePermission = this.thePermissionRepository.findById(id).orElse(null);
        if (thePermission != null) {
            this.thePermissionRepository.delete(thePermission);
        }
    }
}
