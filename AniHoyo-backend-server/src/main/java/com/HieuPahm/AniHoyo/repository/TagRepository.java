package com.HieuPahm.AniHoyo.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.HieuPahm.AniHoyo.model.entities.Tag;

public interface TagRepository extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag> {
    Set<Tag> findByIdIn(List<Long> id);
}
