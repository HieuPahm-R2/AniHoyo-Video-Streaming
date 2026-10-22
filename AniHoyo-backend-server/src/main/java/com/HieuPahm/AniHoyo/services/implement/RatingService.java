package com.HieuPahm.AniHoyo.services.implement;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.HieuPahm.AniHoyo.model.entities.Rating;
import com.HieuPahm.AniHoyo.model.entities.Season;
import com.HieuPahm.AniHoyo.model.entities.User;
import com.HieuPahm.AniHoyo.repository.RatingRepository;
import com.HieuPahm.AniHoyo.repository.SeasonRepository;
import com.HieuPahm.AniHoyo.repository.UserRepository;
import com.HieuPahm.AniHoyo.services.IRatingService;

@Service
public class RatingService implements IRatingService {
    private final UserRepository userRepository;
    private final SeasonRepository seasonRepository;
    private final RatingRepository ratingRepository;

    public RatingService(UserRepository userRepository, SeasonRepository seasonRepository,
            RatingRepository ratingRepository) {
        this.userRepository = userRepository;
        this.seasonRepository = seasonRepository;
        this.ratingRepository = ratingRepository;
    }

    @Override
    public Rating rateMovie(Long userId, Long filmId, int stars) {
        if (stars < 1 || stars > 10)
            throw new IllegalArgumentException("Stars must be between 1 and 10");

        User user = userRepository.findById(userId).orElseThrow();
        Season movie = seasonRepository.findById(filmId).orElseThrow();

        Rating rating = ratingRepository.findByUserIdAndSeasonId(userId, filmId)
                .orElse(new Rating());

        rating.setUser(user);
        rating.setSeason(movie);
        rating.setStars(stars);
        rating.setUpdatedAt(Instant.now());

        return ratingRepository.save(rating);
    }

    @Override
    public Double getAverageStar(Long id) {
        return ratingRepository.getAverageRating(id);
    }

}
