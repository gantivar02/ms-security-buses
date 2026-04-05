package com.AJJ.ms_security.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document
public class Session {
    @Id
    private String id;
    private String token;
    private Date expiration;
    private String code2FA;
    private int attempts; // HU-012: intentos fallidos del código 2FA (máximo 3)

    @DBRef
    private User user;

    public Session(){
    }

    public Session(String code2FA, Date expiration, User user) {
        this.code2FA = code2FA;
        this.expiration = expiration;
        this.user = user;
        this.attempts = 0;
        this.token = null; // el JWT se asigna solo después de verificar el código
    }
}
