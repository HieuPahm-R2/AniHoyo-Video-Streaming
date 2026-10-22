package com.HieuPahm.AniHoyo.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
import com.HieuPahm.AniHoyo.model.dtos.TagDTO;
import com.HieuPahm.AniHoyo.model.entities.Tag;
import com.HieuPahm.AniHoyo.services.implement.TagService;
import com.HieuPahm.AniHoyo.utils.anotation.MessageApi;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping("/add-tag")
    public ResponseEntity<?> addCategory(@Valid @RequestBody TagDTO tagDTO) {
        return ResponseEntity.ok(tagService.insert(tagDTO));
    }

    @PutMapping("/update-tag")
    @MessageApi("Update a tag film")
    public ResponseEntity<?> update(@Valid @RequestBody TagDTO tag) {
        return ResponseEntity.ok().body(tagService.update(tag));
    }

    @GetMapping("/tags")
    @MessageApi("Fetch all tags with pagination")
    public ResponseEntity<PaginationResultDTO> getAllCategories(@Filter Specification<Tag> spec, Pageable pageable) {
        return ResponseEntity.ok().body(this.tagService.getAll(spec, pageable));
    }

    @DeleteMapping("/delete-tag/{id}")
    @MessageApi("Delete a tag film")
    public ResponseEntity<Void> deleteTag(@PathVariable long id) {
        this.tagService.delete(id);
        return ResponseEntity.ok().body(null);
    }
}
