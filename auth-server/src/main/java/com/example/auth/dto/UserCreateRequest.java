package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    private String username;
    private String password;
    private String email;
    private Set<Long> roles = new HashSet<>();
}
