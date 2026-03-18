package com.AJJ.ms_security.Services;


import com.AJJ.ms_security.Models.Role;
import com.AJJ.ms_security.Models.User;
import com.AJJ.ms_security.Models.UserRole;
import com.AJJ.ms_security.Repositories.RoleRepository;
import com.AJJ.ms_security.Repositories.UserRepository;
import com.AJJ.ms_security.Repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserRoleService {
    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    public boolean addUserRole(String userId, String roleId){

        User user=this.theUserRepository.findById(userId).orElse(null);
        Role role=this.theRoleRepository.findById(roleId).orElse(null);

        if(user!=null && role!=null){

            UserRole existing =
                    this.theUserRoleRepository.findByUser_IdAndRole_Id(userId,roleId);

            if(existing!=null){
                return false; // ya existe ese rol para ese usuario
            }

            UserRole theUserRole = new UserRole(user,role);
            this.theUserRoleRepository.save(theUserRole);

            return true;
        }

        return false;
    }

    public boolean removeUserRole(String userRoleId){
        UserRole userRole=this.theUserRoleRepository.findById(userRoleId).orElse(null);
        if (userRole!=null){
            this.theUserRoleRepository.delete(userRole);
            return true;
        }else{
            return false;
        }
    }
    public List<UserRole> getRolesByUser(String userId){
        return this.theUserRoleRepository.getRolesByUser(userId);
    }
    public boolean addMultipleRoles(String userId, List<String> roleIds){

        User user=this.theUserRepository.findById(userId).orElse(null);

        if(user==null){
            return false;
        }

        for(String roleId:roleIds){

            Role role=this.theRoleRepository.findById(roleId).orElse(null);

            if(role!=null){
                UserRole userRole=new UserRole(user,role);
                this.theUserRoleRepository.save(userRole);
            }
        }

        return true;
    }



}