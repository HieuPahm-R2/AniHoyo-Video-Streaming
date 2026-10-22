package com.HieuPahm.AniHoyo.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.turkraft.springfilter.boot.Filter;
import com.HieuPahm.AniHoyo.model.dtos.FilmDTO;
import com.HieuPahm.AniHoyo.model.dtos.PaginationResultDTO;
import com.HieuPahm.AniHoyo.model.entities.Film;
import com.HieuPahm.AniHoyo.repository.FilmRepository;
import com.HieuPahm.AniHoyo.services.implement.FilmService;
import com.HieuPahm.AniHoyo.utils.anotation.MessageApi;
import com.HieuPahm.AniHoyo.utils.error.BadActionException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class FilmController {
    private final FilmService filmService;
    private final FilmRepository filmRepository;

    public FilmController(FilmService filmService, FilmRepository filmRepository) {
        this.filmService = filmService;
        this.filmRepository = filmRepository;
    }

    @PostMapping("/add-film")
    @MessageApi("Add new film")
    public ResponseEntity<?> addFilm(@Valid @RequestBody FilmDTO filmDTO) {
        return ResponseEntity.ok(filmService.insert(filmDTO));
    }

    // # update task
    @PutMapping("/update-film")
    @MessageApi("Edit film info action")
    public ResponseEntity<?> update(@Valid @RequestBody FilmDTO filmDTO) throws BadActionException {
        return ResponseEntity.status(HttpStatus.OK).body(this.filmService.update(filmDTO));
    }

    @DeleteMapping("/delete-film/{id}")
    @MessageApi("Delete a film")
    public ResponseEntity<Void> delete(@PathVariable("id") long id) throws BadActionException {
        if (this.filmRepository.findById(id).isEmpty()) {
            throw new BadActionException("Dữ liệu cần xóa không tìm thấy!");
        }
        this.filmService.delete(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/films")
    @MessageApi("Fetch all data films")
    public ResponseEntity<PaginationResultDTO> getAllFilms(@Filter Specification<Film> spec, Pageable pageable) {
        return ResponseEntity.ok().body(this.filmService.getAll(spec, pageable));
    }

    @GetMapping("/film/{id}")
    @MessageApi("Fetch Film By Id")
    public ResponseEntity<?> getFilm(@PathVariable long id) {
        return ResponseEntity.ok().body(this.filmService.getById(id));
    }

}
