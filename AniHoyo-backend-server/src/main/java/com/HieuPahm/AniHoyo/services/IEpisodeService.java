package com.HieuPahm.AniHoyo.services;

import com.HieuPahm.AniHoyo.model.dtos.EpisodeDTO;
import com.HieuPahm.AniHoyo.utils.error.BadActionException;

public interface IEpisodeService extends ICrudService<EpisodeDTO, Long> {
    /**
     * Runs the FFmpeg/R2 HLS pipeline for one episode. Called on the background
     * worker (and never inline in a request) — it can take minutes.
     */
    public void processVideo(long videoId);

    /**
     * Marks the episode as queued and hands it to the background worker, so
     * {@code POST /api/v1/add-episode} answers immediately.
     */
    public EpisodeDTO queueProcessing(Long id) throws BadActionException;
}
