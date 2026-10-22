package com.HieuPahm.AniHoyo.model.entities;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.HieuPahm.AniHoyo.utils.SecurityUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "films")
public class Film {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(length = 150)
    private String name;
    @Column(length = 150)
    private String studio;
    @Column(length = 4000)
    private String thumbnail;
    @Column(length = 4000)
    private String slider;

    private Instant uploadDate;
    private Instant updatedTime;
    private String uploadBy;
    private String updatedBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "films" })
    @JoinTable(name = "film_category", joinColumns = @JoinColumn(name = "film_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories;

    @OneToMany(mappedBy = "film", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Season> seasons;

    @ManyToMany(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "films" })
    @JoinTable(name = "film_tag", joinColumns = @JoinColumn(name = "film_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags;

    @PrePersist
    public void handleBeforeCreate() {
        this.uploadBy = SecurityUtils.getCurrentUserLogin().isPresent() == true
                ? SecurityUtils.getCurrentUserLogin().get()
                : " ";
        this.uploadDate = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtils.getCurrentUserLogin().isPresent() == true
                ? SecurityUtils.getCurrentUserLogin().get()
                : " ";
        this.updatedTime = Instant.now();
    }
}
