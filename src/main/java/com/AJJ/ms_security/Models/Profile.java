package com.AJJ.ms_security.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Profile {

    @Id
    private String id;
    private String phone;
    private String photo;
    private String address;

    @DBRef
    private User user;

    public Profile() {
    }

    public Profile(String photo, String phone) {
        this.photo = photo;
        this.phone = phone;
    }

}
