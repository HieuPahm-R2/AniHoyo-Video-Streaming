package com.HieuPahm.AniHoyo.model.entities;

import java.time.Instant;
import java.util.List;

import com.HieuPahm.AniHoyo.utils.SecurityUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "permissions")
@NoArgsConstructor
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "Not to blank this field")
    @Size(max = 255, message = "Tên permission tối đa 255 ký tự")
    private String name;
    @NotBlank(message = "Not to blank this field")
    @Size(max = 255, message = "apiPath tối đa 255 ký tự")
    private String apiPath;
    @NotBlank(message = "Not to blank this field")
    @Size(max = 255, message = "method tối đa 255 ký tự")
    private String method;
    @NotBlank(message = "Not to blank this field")
    @Size(max = 255, message = "module tối đa 255 ký tự")
    private String module;

    private Instant createdTime;
    private Instant updatedTime;
    private String createdBy;
    private String updatedBy;

    // handle for Auto create new user with sample permission
    public Permission(String name, String apiPath, String method, String module) {
        this.name = name;
        this.apiPath = apiPath;
        this.method = method;
        this.module = module;
    }

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "permissions")
    @JsonIgnore
    private List<Role> roles;

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
