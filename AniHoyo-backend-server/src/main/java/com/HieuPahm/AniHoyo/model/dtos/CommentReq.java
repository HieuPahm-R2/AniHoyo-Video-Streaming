package com.HieuPahm.AniHoyo.model.dtos;

import com.HieuPahm.AniHoyo.model.entities.Season;
import com.HieuPahm.AniHoyo.model.entities.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentReq {
    private String content;
    private User user;
    private Season season;
    private Long parentId;
}
