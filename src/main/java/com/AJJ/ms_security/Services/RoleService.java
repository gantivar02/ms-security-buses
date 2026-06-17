package com.AJJ.ms_security.Services;

import com.AJJ.ms_security.Models.Role;
import com.AJJ.ms_security.Models.UserRole;
import com.AJJ.ms_security.Repositories.RoleRepository;
import com.AJJ.ms_security.Repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    @Autowired
    private RoleRepository theRoleRepository;
    @Autowired
    private UserRoleRepository theUserRoleRepository;

    public List<Role> find(){
        return this.theRoleRepository.findAll();
    }

    public Role findById(String id){
        return this.theRoleRepository.findById(id).orElse(null);
    }

    public Role create(Role newRole){
        validateRole(newRole, null);
        return this.theRoleRepository.save(newRole);
    }

    public Role update(String id, Role newRole){
        Role actualRole = this.theRoleRepository.findById(id).orElse(null);

        if(actualRole != null){
            validateRole(newRole, id);
            actualRole.setName(newRole.getName());
            actualRole.setDescription(newRole.getDescription());
            this.theRoleRepository.save(actualRole);
            return actualRole;
        } else {
            return null;
        }
    }

    // Retorna false si el rol tiene usuarios asignados o no existe
    public boolean delete(String id) {
        Role theRole = this.theRoleRepository.findById(id).orElse(null);
        if (theRole == null) return false;

        List<UserRole> assignedUsers = this.theUserRoleRepository.getUsersByRole(id);
        if (!assignedUsers.isEmpty()) return false;

        this.theRoleRepository.delete(theRole);
        return true;
    }

    private void validateRole(Role role, String currentId) {
        if (role == null) {
            throw new RuntimeException("Role payload is required");
        }

        if (role.getName() == null || role.getName().trim().isEmpty()) {
            throw new RuntimeException("Role name is required");
        }

        if (role.getDescription() == null || role.getDescription().trim().isEmpty()) {
            throw new RuntimeException("Role description is required");
        }

        role.setName(role.getName().trim());
        role.setDescription(role.getDescription().trim());

        Role byName = this.theRoleRepository.findByNameIgnoreCase(role.getName());
        if (byName != null && !byName.getId().equals(currentId)) {
            throw new RuntimeException("A role with that name already exists");
        }
    }

}
