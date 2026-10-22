package com.HieuPahm.AniHoyo.controller;

import com.HieuPahm.AniHoyo.model.dtos.EpisodeDTO;
import com.HieuPahm.AniHoyo.model.dtos.PaginationResultDTO;
import com.HieuPahm.AniHoyo.model.dtos.ResUpFileDTO;
import com.HieuPahm.AniHoyo.repository.EpisodeRepository;
import com.HieuPahm.AniHoyo.services.implement.EpisodeService;
import com.HieuPahm.AniHoyo.services.implement.FileServiceImpl;
import com.HieuPahm.AniHoyo.services.implement.R2MediaStorageService;
import com.HieuPahm.AniHoyo.utils.anotation.MessageApi;
import com.HieuPahm.AniHoyo.utils.error.BadActionException;
import com.HieuPahm.AniHoyo.utils.error.StorageException;

import jakarta.validation.Valid;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class EpisodeController {
    @Value("${hieupham.upload-file.base-uri}")
    private String baseURI;

    private final EpisodeService episodeService;
    private final EpisodeRepository episodeRepository;
    private final FileServiceImpl fileServiceImpl;
    private final R2MediaStorageService r2MediaStorageService;

    public EpisodeController(EpisodeService episodeService, EpisodeRepository episodeRepository,
            FileServiceImpl fileServiceImpl, R2MediaStorageService r2MediaStorageService) {
        this.episodeService = episodeService;
        this.episodeRepository = episodeRepository;
        this.fileServiceImpl = fileServiceImpl;
        this.r2MediaStorageService = r2MediaStorageService;
    }

    @PostMapping("/add-episode")
    @MessageApi("Thêm tập phim (video được chuyển mã HLS ở nền)")
    public ResponseEntity<?> createEpisode(@Valid @RequestBody EpisodeDTO dto) throws BadActionException {
        if (this.episodeService.checkExistSeason(dto.getSeason().getId()) == false) {
            throw new BadActionException("Not Found this season");
        }
        // Returns as soon as the row is stored; FFmpeg output is produced by the
        // background worker and reported through EpisodeDTO#hlsStatus.
        return ResponseEntity.status(HttpStatus.CREATED).body(this.episodeService.insert(dto));
    }

    /**
     * Re-runs the HLS pipeline for one episode: the fix for a failed run
     * (see {@code hlsError}) and for rows created before async processing.
     * The R2 key is derived from the source key, so re-running is idempotent.
     */
    @PostMapping("/process-episode/{id}")
    @MessageApi("Xử lý lại HLS cho tập phim")
    public ResponseEntity<EpisodeDTO> processEpisode(@PathVariable("id") long id) throws BadActionException {
        return ResponseEntity.accepted().body(this.episodeService.queueProcessing(id));
    }

    @PostMapping("/update-episode")
    public ResponseEntity<?> update(@Valid @RequestBody EpisodeDTO dto) throws BadActionException {
        return ResponseEntity.ok().body(this.episodeService.update(dto));
    }

    @DeleteMapping("/delete-episode/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") long id) throws BadActionException {
        if (this.episodeRepository.findById(id).isEmpty()) {
            throw new BadActionException("Not Found this episode");
        }
        this.episodeService.delete(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/episodes/by-season/{seasonId}")
    @MessageApi("fetch all eps of season")
    public ResponseEntity<PaginationResultDTO> fetchEpsBySeason(@PathVariable Long seasonId, Pageable pageable) {
        return ResponseEntity.ok().body(this.episodeService.fetchEpsBySeason(seasonId, pageable));
    }

    @PostMapping("/upload/video")
    @MessageApi("Upload an Episode")
    public ResponseEntity<ResUpFileDTO> uploadEpisode(
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam("folder") String folder) throws URISyntaxException, IOException, StorageException {
        // CHECK VALIDATE
        if (file == null || file.isEmpty()) {
            throw new StorageException("Not leave blank, Please upload!!");
        }
        String fileName = file.getOriginalFilename();
        List<String> extensionsAllowed = Arrays.asList("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv");
        boolean isValid = extensionsAllowed.stream().anyMatch(item -> fileName.toLowerCase().endsWith(item));
        if (!isValid) {
            throw new StorageException("Invalid file format, Please try again!");
        }
        String fileUpload;
        if (r2MediaStorageService.isEnabled()) {
            // R2 originals stay private; the client never receives this object URL.
            fileUpload = r2MediaStorageService.storeSourceVideo(file);
        } else {
            // Must be storeLocalFile, not storeFile: processVideoLocally runs FFmpeg
            // on <base-uri>/<folder>/<name>, while storeFile writes to SeaweedFS as
            // soon as that store is enabled (images), which FFmpeg never reads.
            fileUpload = this.fileServiceImpl.storeLocalFile(file, folder);
        }
        ResUpFileDTO result = new ResUpFileDTO(fileUpload, Instant.now());

        return ResponseEntity.ok().body(result);
    }
    // streaming video with chunked method

}
