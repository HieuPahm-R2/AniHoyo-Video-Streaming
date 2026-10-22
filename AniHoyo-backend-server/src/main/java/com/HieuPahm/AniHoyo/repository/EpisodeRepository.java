package com.HieuPahm.AniHoyo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.HieuPahm.AniHoyo.model.entities.Episode;
import com.HieuPahm.AniHoyo.model.entities.Season;
import com.HieuPahm.AniHoyo.utils.constant.HlsStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EpisodeRepository extends JpaRepository<Episode, Long>, JpaSpecificationExecutor<Episode> {
    Optional<Episode> findByTitle(String title);

    // query methods
    List<Episode> findBySeason(Season season);

    /** Episodes whose HLS pipeline is unfinished (queued or interrupted by a restart). */
    List<Episode> findByHlsStatusIn(Collection<HlsStatus> statuses);
}
