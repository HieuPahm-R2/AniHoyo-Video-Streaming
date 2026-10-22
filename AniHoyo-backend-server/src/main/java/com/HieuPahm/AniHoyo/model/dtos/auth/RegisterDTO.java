package com.HieuPahm.AniHoyo.model.dtos.auth;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RegisterDTO {
    private long id;

    private String fullName;

    private String email;

    private String password;
    private Instant updatedTime;
    private Instant createdTime;
}
