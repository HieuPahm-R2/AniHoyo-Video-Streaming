package com.HieuPahm.AniHoyo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.HieuPahm.AniHoyo.model.entities.Rating;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndSeasonId(Long userId, Long seasonId);

    List<Rating> findBySeasonId(Long movieId);

    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.season.id = :seasonId")
    Double getAverageRating(@Param("seasonId") Long seasonId);
}
