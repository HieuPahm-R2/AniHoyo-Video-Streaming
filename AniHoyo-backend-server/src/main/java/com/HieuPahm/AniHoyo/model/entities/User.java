package com.HieuPahm.AniHoyo.model.entities;

import java.time.Instant;
import java.util.List;

import com.HieuPahm.AniHoyo.utils.SecurityUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Name not be blank..")
    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    private String fullName;

    @NotBlank(message = "Email not be blank..")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password not be blank..")
    @Size(max = 100, message = "Mật khẩu tối đa 100 ký tự")
    private String password;

    /** Avatar URL produced by the image storage (can exceed 255 chars). */
    @Column(length = 1000)
    private String avatar;

    private Instant createdTime;
    private Instant updatedTime;

    private String createdBy;
    private String updatedBy;

    // db relationship
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Rating> rates;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Comment> comments;

    // ===============
    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtils.getCurrentUserLogin().isPresent() == true
                ? SecurityUtils.getCurrentUserLogin().get()
                : " ";
        this.createdTime = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtils.getCurrentUserLogin().isPresent() == true
                ? SecurityUtils.getCurrentUserLogin().get()
                : " ";
        this.updatedTime = Instant.now();
    }
}
