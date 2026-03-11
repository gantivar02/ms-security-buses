package com.AJJ.ms_security.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class UserRole {
    @Id
    private String id;

    @DBRef
    private User user;
    @DBRef
    private Role role;

    public UserRole(){
    }

    public UserRole(User user, Role role){
        this.user=user;
        this.role=role;
    }
}