package com.HieuPahm.AniHoyo.model.dtos;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilmDTO {
    private long id;

    @NotBlank(message = "Tên phim không được để trống")
    @Size(max = 150, message = "Tên phim tối đa 150 ký tự")
    private String name;

    @Size(max = 150, message = "Tên studio tối đa 150 ký tự")
    private String studio;

    @Size(max = 4000, message = "Đường dẫn thumbnail tối đa 4000 ký tự")
    private String thumbnail;

    @Size(max = 4000, message = "Đường dẫn slider tối đa 4000 ký tự")
    private String slider;

    private Set<CategoryDTO> categories;
    private List<SeasonDTO> seasons;
    private Set<TagDTO> tags;

    private Instant uploadDate;
}
