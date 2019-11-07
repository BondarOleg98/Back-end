package com.netcraker.model;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class User {
    private int userId;
    private String full_name;
    private String email;
    private String password;
    private Timestamp createdAt;
    private boolean enabled;
    private String photoPath;
}
