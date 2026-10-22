package com.HieuPahm.AniHoyo.model.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TagDTO {
    private Long id;
    @NotBlank(message = "Tên tag không được để trống")
    @Size(max = 255, message = "Tên tag tối đa 255 ký tự")
    private String name;

    public TagDTO(Long id) {
        this.id = id;
    }
}
