package com.HieuPahm.AniHoyo.model.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDTO {
    private Long id;
    @NotBlank(message = "Tên thể loại không được để trống")
    @Size(max = 150, message = "Tên thể loại tối đa 150 ký tự")
    private String name;

    public CategoryDTO(Long id) {
        this.id = id;
    }
}
