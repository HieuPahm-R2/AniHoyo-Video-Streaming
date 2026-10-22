package com.HieuPahm.AniHoyo.controller;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.HieuPahm.AniHoyo.model.dtos.PaginationResultDTO;
import com.HieuPahm.AniHoyo.model.dtos.SeasonDTO;
import com.HieuPahm.AniHoyo.model.entities.Season;
import com.HieuPahm.AniHoyo.services.implement.SeasonService;
import com.HieuPahm.AniHoyo.utils.SecurityUtils;
import com.HieuPahm.AniHoyo.utils.anotation.MessageApi;
import com.HieuPahm.AniHoyo.utils.error.BadActionException;
import com.turkraft.springfilter.boot.Filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SeasonController {
    private static final String VIEWER_COOKIE = "anihoyo_viewer";
    private final SeasonService seasonService;
    @Value("${anihoyo.views.cookie-secure:false}")
    private boolean secureViewerCookie;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @PostMapping("/add-season")
    @MessageApi("add a season")
    public ResponseEntity<?> addSeason(@Valid @RequestBody SeasonDTO dto) {
        return ResponseEntity.ok().body(this.seasonService.insert(dto));
    }

    @PutMapping("/update-season")
    @MessageApi("update a season")
    public ResponseEntity<?> update(@Valid @RequestBody SeasonDTO dto) throws BadActionException {
        return ResponseEntity.ok().body(this.seasonService.update(dto));
    }

    @GetMapping("/season/{id}")
    @MessageApi("Fetch Season By id")
    public ResponseEntity<?> getSeason(@PathVariable long id) {
        return ResponseEntity.ok().body(this.seasonService.getById(id));
    }

    @DeleteMapping("/delete-season/{id}")
    @MessageApi("delete a season")
    public ResponseEntity<Void> delete(@PathVariable("id") long id) {
        this.seasonService.delete(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/seasons")
    public ResponseEntity<PaginationResultDTO> getAllCategories(@Filter Specification<Season> spec,
            Pageable pageable) {
        return ResponseEntity.ok().body(this.seasonService.fetchAll(spec, pageable));
    }

    @GetMapping("/seasons/by-film/{filmId}")
    @MessageApi("fetch all seasons of film")
    public ResponseEntity<PaginationResultDTO> fetchSeasonsByFilm(@PathVariable Long filmId, Pageable pageable) {
        return ResponseEntity.ok().body(this.seasonService.fetchSeasonsByFilm(filmId, pageable));
    }

    @GetMapping("/seasons/top-views")
    @MessageApi("fetch top 5 seasons by views")
    public ResponseEntity<List<SeasonDTO>> getTop5SeasonsByViews() {
        return ResponseEntity.ok().body(this.seasonService.getTop5SeasonsByViews());
    }

    @GetMapping("/seasons/related/{seasonId}")
    @MessageApi("fetch all seasons of the same film")
    public ResponseEntity<List<SeasonDTO>> getRelatedSeasons(@PathVariable Long seasonId) {
        return ResponseEntity.ok().body(this.seasonService.getRelatedSeasons(seasonId));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> increaseView(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) {
        String viewerId = SecurityUtils.getCurrentUserLogin()
                .map(email -> "account:" + UUID.nameUUIDFromBytes(email.getBytes(StandardCharsets.UTF_8)))
                .orElseGet(() -> visitorId(request, response));
        seasonService.increaseViewOnce(id, viewerId);
        return ResponseEntity.ok().build();
    }

    private String visitorId(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (VIEWER_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    try {
                        return "visitor:" + UUID.fromString(cookie.getValue());
                    } catch (IllegalArgumentException ignored) {
                        // Ignore forged or malformed identifiers and issue a fresh opaque cookie.
                    }
                }
            }
        }

        String visitorId = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(VIEWER_COOKIE, visitorId)
                .httpOnly(true)
                .secure(secureViewerCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return "visitor:" + visitorId;
    }
}
