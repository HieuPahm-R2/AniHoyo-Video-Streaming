package com.HieuPahm.AniHoyo.model.dtos;

import java.time.Instant;
import java.util.List;

import com.HieuPahm.AniHoyo.model.entities.Episode;
import com.HieuPahm.AniHoyo.model.entities.Film;
import com.HieuPahm.AniHoyo.utils.constant.GenersEnum;
import com.HieuPahm.AniHoyo.utils.constant.StatusEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeasonDTO {
    private Long id;
    @NotBlank(message = "Tên season không được để trống")
    @Size(max = 150, message = "Tên season tối đa 150 ký tự")
    private String seasonName;
    @Size(max = 255, message = "Ordinal tối đa 255 ký tự")
    private String ordinal;
    // Bounds mirror the columns of `seasons` (see deploy/mysql/migrations): failing
    // here returns a readable 400 instead of a MySQL "Data too long" error.
    @Size(max = 1000, message = "Đường dẫn thumbnail tối đa 1000 ký tự")
    private String thumb;
    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description;
    private Integer releaseYear;
    private Instant uploadDate;
    private GenersEnum type;
    private StatusEnum status;
    @Size(max = 1000, message = "Link trailer tối đa 1000 ký tự")
    private String trailer;
    private Long viewCount;
    private Film film;
    private List<Episode> episodes;
}
