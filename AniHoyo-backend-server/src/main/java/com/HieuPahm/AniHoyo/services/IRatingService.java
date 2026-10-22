package com.HieuPahm.AniHoyo.services;

import com.HieuPahm.AniHoyo.model.entities.Rating;

public interface IRatingService {
    public Rating rateMovie(Long userId, Long filmId, int stars);

    public Double getAverageStar(Long id);
}
