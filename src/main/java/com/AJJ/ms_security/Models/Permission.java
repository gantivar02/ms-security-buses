package com.AJJ.ms_security.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Permission {
    @Id
    private String id;

    private String name;         // users_read
    private String module;       // users
    private String action;       // read
    private String description;  // Permite consultar usuarios

    private String url;          // /users
    private String method;       // GET

    public Permission() {
    }

    public Permission(String name, String module, String action, String description, String url, String method) {
        this.name = name;
        this.module = module;
        this.action = action;
        this.description = description;
        this.url = url;
        this.method = method;
    }
}