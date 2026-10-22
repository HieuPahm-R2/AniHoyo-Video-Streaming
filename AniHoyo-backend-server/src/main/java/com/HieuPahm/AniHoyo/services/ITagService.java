package com.HieuPahm.AniHoyo.services;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.HieuPahm.AniHoyo.model.dtos.PaginationResultDTO;
import com.HieuPahm.AniHoyo.model.dtos.TagDTO;
import com.HieuPahm.AniHoyo.model.entities.Tag;

public interface ITagService extends ICrudService<TagDTO, Long> {
    PaginationResultDTO getAll(Specification<Tag> spec, Pageable pageable);
}
