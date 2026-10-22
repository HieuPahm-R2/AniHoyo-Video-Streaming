package com.HieuPahm.AniHoyo.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.HieuPahm.AniHoyo.model.dtos.ResUpFileDTO;
import com.HieuPahm.AniHoyo.services.implement.FileServiceImpl;
import com.HieuPahm.AniHoyo.utils.anotation.MessageApi;
import com.HieuPahm.AniHoyo.utils.error.StorageException;

@RestController
@RequestMapping("/api/v1")
public class FileController {
    @Value("${hieupham.upload-file.base-uri}")
    private String baseURI;

    private final FileServiceImpl fileService;

    public FileController(FileServiceImpl fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/files")
    @MessageApi("Upload Single file")
    public ResponseEntity<ResUpFileDTO> uploadData(
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam("folder") String folder) throws URISyntaxException, IOException, StorageException {
        // CHECK VALIDATE
        if (file == null || file.isEmpty()) {
            throw new StorageException("Not leave blank, Please upload!!");
        }
        String fileName = file.getOriginalFilename();
        List<String> extensionsAllowed = Arrays.asList("pdf", "jpeg", "png", "webp", "jpg");
        boolean isValid = extensionsAllowed.stream().anyMatch(item -> fileName.toLowerCase().endsWith(item));
        if (!isValid) {
            throw new StorageException("Invalid file format, Please try again!");
        }
        // handle create folder (Option)
        this.fileService.createFolder(baseURI + folder);
        String fileUpload = this.fileService.storeFile(file, folder);
        ResUpFileDTO result = new ResUpFileDTO(fileUpload, Instant.now());

        return ResponseEntity.ok().body(result);
    }
}
