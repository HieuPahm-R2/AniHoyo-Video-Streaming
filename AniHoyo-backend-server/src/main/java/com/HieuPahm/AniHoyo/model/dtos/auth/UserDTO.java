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
public class UserDTO {
    private long id;
    private String fullName;
    private String email;
    private String avatar;

    private Instant updatedTime;
    private Instant createdTime;

    private RoleOfUser role;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoleOfUser {
        private long id;
        private String name;
    }
}
