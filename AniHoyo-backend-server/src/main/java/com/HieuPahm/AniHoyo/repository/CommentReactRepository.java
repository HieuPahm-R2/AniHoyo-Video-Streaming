package com.HieuPahm.AniHoyo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.HieuPahm.AniHoyo.model.entities.CommentReact;

public interface CommentReactRepository extends JpaRepository<CommentReact, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    long countByCommentId(Long commentId);
}
