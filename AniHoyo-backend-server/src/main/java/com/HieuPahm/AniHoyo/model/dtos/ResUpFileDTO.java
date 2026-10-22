package com.HieuPahm.AniHoyo.model.dtos;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResUpFileDTO {
    private String fileName;
    private Instant timeUpload;
}
