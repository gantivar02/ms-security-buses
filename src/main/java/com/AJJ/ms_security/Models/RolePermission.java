package com.AJJ.ms_security.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class RolePermission {
    @Id
    private String id;

    @DBRef
    private Permission permission;
    @DBRef
    private Role role;

    public RolePermission(){
    }

    public RolePermission(Permission permission, Role role){
        this.permission=permission;
        this.role=role;
    }
}
