package com.HieuPahm.AniHoyo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.HieuPahm.AniHoyo.model.entities.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findBySeasonIdAndParentIsNullOrderByCreatedAtDesc(Long seasonId);
}