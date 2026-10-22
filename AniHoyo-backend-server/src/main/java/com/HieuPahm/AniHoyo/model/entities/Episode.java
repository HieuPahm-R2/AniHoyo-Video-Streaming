package com.HieuPahm.AniHoyo.model.entities;

import com.HieuPahm.AniHoyo.utils.constant.HlsStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "episodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Episode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 150)
    private String title;

    /** Storage path/URL of the media file; R2 keys + HLS playlists exceed 255 chars. */
    @Column(length = 1000)
    private String filePath;
    private String contentType;

    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;

    /**
     * State of the HLS pipeline. {@code null} on rows that predate the async
     * pipeline: playback then falls back to checking the media bucket once.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "hls_status", length = 20)
    private HlsStatus hlsStatus;

    /** Last FFmpeg/R2 failure, so the admin can see why an episode is not playable. */
    @Column(name = "hls_error", length = 500)
    private String hlsError;
}
