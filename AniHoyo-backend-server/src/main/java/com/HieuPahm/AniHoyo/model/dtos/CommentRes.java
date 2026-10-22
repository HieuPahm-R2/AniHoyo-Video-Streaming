package com.HieuPahm.AniHoyo.model.dtos;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CommentRes {
    private Long id;
    private Long userId;
    private String fullName;
    private String content;
    private int likeCount;
    private List<CommentRes> replies;
    private Instant createdAt;
}
