package com.AJJ.ms_security.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document
public class PasswordResetToken {

    @Id
    private String id;

    private String token;
    private Date expiration;
    private boolean used;

    @DBRef
    private User user;

    public PasswordResetToken() {
    }

    public PasswordResetToken(String token, Date expiration, User user) {
        this.token = token;
        this.expiration = expiration;
        this.user = user;
        this.used = false;
    }
}
