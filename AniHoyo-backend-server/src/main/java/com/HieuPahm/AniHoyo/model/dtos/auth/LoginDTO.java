package com.HieuPahm.AniHoyo.model.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginDTO {
    @NotBlank(message = "username not be blank")
    private String username;
    @NotBlank(message = "mật khẩu không được để trống")
    private String password;
}
