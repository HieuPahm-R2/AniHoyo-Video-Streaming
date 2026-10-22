package com.HieuPahm.AniHoyo.model.dtos;

import java.time.Instant;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadFilmDTO {
    private long id;

    private String name;

    private String studio;

    private String thumbnail;

    private String description;

    private int releaseYear;

    private boolean status;

    private Instant uploadDate;

    private Set<CategoryDTO> categories;
    private Set<TagDTO> tags;
}
