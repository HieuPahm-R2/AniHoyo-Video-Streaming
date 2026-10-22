package com.HieuPahm.AniHoyo.model.entities;

import java.time.Instant;
import java.util.List;

import com.HieuPahm.AniHoyo.utils.SecurityUtils;
import com.HieuPahm.AniHoyo.utils.constant.GenersEnum;
import com.HieuPahm.AniHoyo.utils.constant.StatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seasons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Season {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(length = 150)
    private String seasonName;
    private String ordinal;
    /**
     * Absolute URLs returned by the object storage (Cloudflare R2 / SeaweedFS) are
     * routinely longer than 255 characters; a 255-char column turns that into
     * "Data too long for column ...", i.e. a 500 on a perfectly valid upload.
     */
    @Column(length = 1000)
    private String thumb;
    @Column(length = 1000)
    private String trailer;
    private Long viewCount;
    private Integer releaseYear;

    private Instant uploadDate;

    @Column(length = 4000)
    private String description;

    private String createdBy;
    private String updatedBy;

    @Enumerated(EnumType.STRING)
    private GenersEnum type;

    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @ManyToOne
    @JoinColumn(name = "film_id")
    private Film film;

    @OneToMany(mappedBy = "season", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Episode> episodes;

    @OneToMany(mappedBy = "season", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Rating> rates;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtils.getCurrentUserLogin().isPresent() == true
                ? SecurityUtils.getCurrentUserLogin().get()
                : " ";
        this.uploadDate = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtils.getCurrentUserLogin().isPresent() == true
                ? SecurityUtils.getCurrentUserLogin().get()
                : " ";
        this.uploadDate = Instant.now();
    }
}
