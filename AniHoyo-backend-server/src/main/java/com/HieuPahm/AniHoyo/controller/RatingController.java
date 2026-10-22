package com.HieuPahm.AniHoyo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.HieuPahm.AniHoyo.model.entities.Rating;
import com.HieuPahm.AniHoyo.services.implement.RatingService;

@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController {
    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public ResponseEntity<Rating> rateMovie(@RequestParam Long userId,
            @RequestParam Long seasonId,
            @RequestParam int stars) {
        return ResponseEntity.ok(ratingService.rateMovie(userId, seasonId, stars));
    }

    @GetMapping("/average/{seasonId}")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long seasonId) {
        return ResponseEntity.ok(ratingService.getAverageStar(seasonId));
    }
}
