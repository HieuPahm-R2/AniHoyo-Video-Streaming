package com.HieuPahm.AniHoyo.services.implement;

import com.HieuPahm.AniHoyo.model.dtos.EpisodeDTO;
import com.HieuPahm.AniHoyo.model.dtos.PaginationResultDTO;
import com.HieuPahm.AniHoyo.model.entities.Episode;
import com.HieuPahm.AniHoyo.model.entities.Season;
import com.HieuPahm.AniHoyo.repository.EpisodeRepository;
import com.HieuPahm.AniHoyo.repository.SeasonRepository;
import com.HieuPahm.AniHoyo.services.IEpisodeService;
import com.HieuPahm.AniHoyo.utils.error.BadActionException;
import com.turkraft.springfilter.builder.FilterBuilder;
import com.turkraft.springfilter.converter.FilterSpecification;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import com.turkraft.springfilter.parser.node.FilterNode;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.HieuPahm.AniHoyo.config.HlsProcessingConfig;
import com.HieuPahm.AniHoyo.utils.constant.HlsStatus;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EpisodeService implements IEpisodeService {
    @Autowired
    FilterBuilder fb;
    @Autowired
    private FilterParser filterParser;
    @Autowired
    private FilterSpecificationConverter filterSpecificationConverter;
    @Value("${hieupham.upload-file.base-uri}")
    private String baseURI;
    @Value("${hieupham.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;
    @Value("${hieupham.ffprobe-path:ffprobe}")
    private String ffprobePath;

    private final EpisodeRepository episodeRepository;
    private final SeasonRepository seasonRepository;
    private final ModelMapper modelMapper;
    private final R2MediaStorageService r2MediaStorageService;
    private final Executor hlsExecutor;

    /**
     * Episode ids currently queued or running, so a double click cannot transcode
     * twice.
     * addt phần tử vào Set,
     * nó sẽ làm Key của map, còn Value là một giá trị cố định
     * (Boolean.TRUE).
     */
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public EpisodeService(EpisodeRepository episodeRepository, SeasonRepository seasonRepository,
            ModelMapper modelMapper, R2MediaStorageService r2MediaStorageService,
            @Qualifier(HlsProcessingConfig.HLS_EXECUTOR) Executor hlsExecutor) {
        this.episodeRepository = episodeRepository;
        this.seasonRepository = seasonRepository;
        this.modelMapper = modelMapper;
        this.r2MediaStorageService = r2MediaStorageService;
        this.hlsExecutor = hlsExecutor;
    }

    @Override
    public EpisodeDTO insert(EpisodeDTO dto) {
        Episode ep = modelMapper.map(dto, Episode.class);
        ep.setHlsStatus(HlsStatus.PENDING);
        ep.setHlsError(null);
        ep = this.episodeRepository.save(ep);
        // Encoding happens on the worker thread: the request must not hold the
        // connection open for the minutes FFmpeg needs (that used to surface as a
        // 500 + a client timeout while the episode row was already committed).
        enqueueProcessing(ep.getId());
        return modelMapper.map(ep, EpisodeDTO.class);
    }

    @Override
    public EpisodeDTO queueProcessing(Long id) throws BadActionException {
        Episode episode = this.episodeRepository.findById(id)
                .orElseThrow(() -> new BadActionException("Not Found this episode"));
        if (episode.getHlsStatus() != HlsStatus.READY) {
            episode.setHlsStatus(HlsStatus.PENDING);
        }
        episode.setHlsError(null);
        this.episodeRepository.save(episode);
        enqueueProcessing(id);
        return modelMapper.map(episode, EpisodeDTO.class);
    }

    private void enqueueProcessing(long id) {
        if (!inFlight.add(id)) {
            log.info("HLS processing for episode {} is already queued/running, skipping.", id);
            return;
        }
        try {
            hlsExecutor.execute(() -> {
                try {
                    processVideo(id);
                } finally {
                    inFlight.remove(id);
                }
            });
        } catch (RejectedExecutionException ex) {
            inFlight.remove(id);
            log.error("HLS queue is full, episode {} was not scheduled.", id, ex);
            markStatus(id, HlsStatus.FAILED, "Hàng đợi xử lý video đang đầy, vui lòng thử lại sau.");
        }
    }

    /**
     * Re-queues pipelines that were interrupted by a restart (their row is stuck
     * in PENDING/PROCESSING while no worker is running anymore).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void resumeInterruptedProcessing() {
        List<Episode> pending = this.episodeRepository
                .findByHlsStatusIn(List.of(HlsStatus.PENDING, HlsStatus.PROCESSING));
        for (Episode episode : pending) {
            log.info("Re-queueing interrupted HLS processing for episode {}.", episode.getId());
            enqueueProcessing(episode.getId());
        }
    }

    @Override
    @Cacheable(value = "episodes", key = "#id")
    public EpisodeDTO getById(Long id) {
        Optional<Episode> ep = this.episodeRepository.findById(id);
        return modelMapper.map(ep.get(), EpisodeDTO.class);
    }

    @Override
    @CacheEvict(value = "episodes", key = "#dto.id")
    public EpisodeDTO update(EpisodeDTO dto) throws BadActionException {
        Optional<Episode> ep = this.episodeRepository.findById(dto.getId());
        if (ep.isEmpty()) {
            // Used to save the entity and *then* throw, so the API reported a
            // failure for a change that had already been written.
            throw new BadActionException("Not Found this ep");
        }
        boolean sourceChanged = !Objects.equals(ep.get().getFilePath(), dto.getFilePath());
        ep.get().setTitle(dto.getTitle());
        ep.get().setFilePath(dto.getFilePath());
        ep.get().setContentType(dto.getContentType());
        if (sourceChanged) {
            // A new source means a new HLS prefix on R2, so it has to be encoded again.
            ep.get().setHlsStatus(HlsStatus.PENDING);
            ep.get().setHlsError(null);
        }
        Episode saved = this.episodeRepository.save(ep.get());
        if (sourceChanged) {
            enqueueProcessing(saved.getId());
        }
        return modelMapper.map(saved, EpisodeDTO.class);
    }

    @Override
    @CacheEvict(value = "episodes", key = "#id")
    public void delete(Long id) {
        this.episodeRepository.deleteById(id);
    }

    public PaginationResultDTO fetchEpsBySeason(Long seasonId, Pageable pageable) {
        FilterNode node = filterParser.parse("season.id=" + seasonId);
        FilterSpecification<Episode> spec = filterSpecificationConverter.convert(node);
        Page<Episode> pageCheck = this.episodeRepository.findAll(spec, pageable);

        PaginationResultDTO ans = new PaginationResultDTO();
        PaginationResultDTO.Meta mt = new PaginationResultDTO.Meta();

        mt.setPage(pageCheck.getNumber() + 1);
        mt.setPageSize(pageCheck.getSize());
        mt.setTotal(pageCheck.getTotalElements());
        mt.setPages(pageCheck.getTotalPages());
        ans.setMeta(mt);

        List<EpisodeDTO> res = pageCheck.getContent().stream()
                .map(item -> modelMapper.map(item, EpisodeDTO.class))
                .collect(Collectors.toList());
        ans.setResult(res);
        return ans;
    }

    public boolean checkExistSeason(Long id) {
        Optional<Season> check = this.seasonRepository.findById(id);
        if (check.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public void processVideo(long id) {
        Optional<Episode> found = this.episodeRepository.findById(id);
        if (found.isEmpty()) {
            log.warn("HLS processing requested for unknown episode {}.", id);
            return;
        }
        Episode episode = found.get();
        markStatus(id, HlsStatus.PROCESSING, null);
        log.info("HLS processing started for episode {} (source: {}).", id, episode.getFilePath());
        try {
            if (r2MediaStorageService.isEnabled()) {
                processVideoWithR2(episode);
            } else {
                processVideoLocally(episode);
            }
            markStatus(id, HlsStatus.READY, null);
            log.info("HLS processing finished for episode {}.", id);
        } catch (Exception ex) {
            log.error("HLS processing failed for episode {}.", id, ex);
            markStatus(id, HlsStatus.FAILED, rootMessage(ex));
        }
    }

    /**
     * Non-R2 fallback: encode next to the original inside the local upload volume.
     */
    private void processVideoLocally(Episode episode) {
        URI inputUri = URI.create(baseURI + "videos/" + episode.getFilePath());
        Path inputVideoPath = Paths.get(inputUri);
        if (!Files.isRegularFile(inputVideoPath)) {
            // Without this check FFmpeg fails with a wall of banner text and the real
            // reason ("the upload never landed in the local volume") is lost.
            throw new IllegalStateException("Không tìm thấy file gốc: " + inputVideoPath
                    + ". Khi R2_ENABLED=false, video phải nằm trong volume uploads"
                    + " (kiểm tra POST /api/v1/upload/video có trả về tên file local không).");
        }
        Path outputRoot = Paths.get(URI.create(baseURI + "videos_hls/" + episode.getTitle() + "/"));
        try {
            encodeHls(inputVideoPath, outputRoot);
        } catch (IOException ex) {
            throw new RuntimeException("processing failed !!\n" + ex.getMessage(), ex);
        }
    }

    private void markStatus(long id, HlsStatus status, String error) {
        try {
            this.episodeRepository.findById(id).ifPresent(episode -> {
                episode.setHlsStatus(status);
                episode.setHlsError(truncate(error));
                this.episodeRepository.save(episode);
            });
        } catch (RuntimeException ex) {
            // Never let bookkeeping hide the actual pipeline result.
            log.error("Could not persist HLS status {} for episode {}.", status, id, ex);
        }
    }

    /**
     * Column is 500 chars wide; a raw FFmpeg log would otherwise abort the update.
     */
    private static String truncate(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 497) + "...";
    }

    /** Deepest cause message: FFmpeg/awssdk put the useful text at the bottom. */
    private static String rootMessage(Throwable ex) {
        Throwable cause = ex;
        String message = ex.getMessage();
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                message = cause.getMessage();
            }
        }
        return message != null ? message : ex.getClass().getSimpleName();
    }

    private void processVideoWithR2(Episode episode) {
        Path inputVideoPath = null;
        Path outputRoot = null;
        try {
            inputVideoPath = r2MediaStorageService.downloadSourceVideo(episode.getFilePath());
            outputRoot = Files.createTempDirectory("anihoyo-hls-" + episode.getId() + "-");
            encodeHls(inputVideoPath, outputRoot);
            r2MediaStorageService.uploadHlsDirectory(episode, outputRoot);
        } catch (IOException ex) {
            throw new RuntimeException("R2 video processing failed !!\n" + ex.getMessage(), ex);
        } finally {
            if (inputVideoPath != null) {
                try {
                    Files.deleteIfExists(inputVideoPath);
                } catch (IOException ignored) {
                    // Temporary source cleanup is best effort.
                }
            }
            r2MediaStorageService.deleteDirectory(outputRoot);
        }
    }

    private void encodeHls(Path inputVideoPath, Path outputRoot) throws IOException {
        Files.createDirectories(outputRoot);

        int sourceWidth = 0;
        int sourceHeight = 0;
        try {
            int[] wh = probeVideoResolution(inputVideoPath);
            sourceWidth = wh[0];
            sourceHeight = wh[1];
        } catch (Exception ignore) {
            // keep defaults (0,0) to fall back to 360p only
        }

        List<Integer> targetHeights = new ArrayList<>();
        targetHeights.add(360);
        if (sourceHeight >= 720) {
            targetHeights.add(720);
        }
        if (sourceHeight >= 1080) {
            targetHeights.add(1080);
        }

        List<String> masterEntries = new ArrayList<>();
        for (Integer height : targetHeights) {
            Path variantDir = outputRoot.resolve(height + "p");
            Files.createDirectories(variantDir);

            int outWidth;
            if (sourceWidth > 0 && sourceHeight > 0) {
                outWidth = Math.max(2, ((sourceWidth * height) / sourceHeight) & ~1);
            } else {
                outWidth = Math.max(2, ((16 * height) / 9) & ~1);
            }

            Path segmentPath = variantDir.resolve("segment_%03d.ts");
            Path playlistPath = variantDir.resolve("index.m3u8");

            runFfmpegToHls(inputVideoPath, segmentPath, playlistPath, height);

            long bandwidth;
            if (height >= 1080) {
                bandwidth = 5000000L;
            } else if (height >= 720) {
                bandwidth = 2500000L;
            } else {
                bandwidth = 800000L;
            }
            masterEntries.add("#EXT-X-STREAM-INF:BANDWIDTH=" + bandwidth + ",RESOLUTION=" + outWidth + "x" + height
                    + "\n" + height + "p/index.m3u8");
        }

        StringBuilder master = new StringBuilder();
        master.append("#EXTM3U\n");
        master.append("#EXT-X-VERSION:3\n");
        for (String entry : masterEntries) {
            master.append(entry).append("\n");
        }
        Files.write(outputRoot.resolve("master.m3u8"), master.toString().getBytes(StandardCharsets.UTF_8));
    }

    private int[] probeVideoResolution(Path inputVideoPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                ffprobePath,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=p=0:s=x",
                inputVideoPath.toString());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("ffprobe failed: " + output);
        }
        String result = output.toString().trim();
        String[] parts = result.split("x");
        int width = Integer.parseInt(parts[0].trim());
        int height = Integer.parseInt(parts[1].trim());
        return new int[] { width, height };
    }

    private void runFfmpegToHls(Path inputVideoPath, Path segmentPath, Path playlistPath, int targetHeight)
            throws IOException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    ffmpegPath,
                    "-nostdin",
                    "-y",
                    "-i", inputVideoPath.toString(),
                    "-vf", "scale=-2:" + targetHeight,
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-crf", "23",
                    "-c:a", "aac",
                    "-f", "hls",
                    "-hls_time", "10",
                    "-hls_list_size", "0",
                    "-hls_playlist_type", "vod",
                    "-hls_segment_filename", segmentPath.toString(),
                    playlistPath.toString());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("ffmpeg exit " + exit + " (" + targetHeight + "p): " + tail(output));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * FFmpeg prints its whole build banner (hundreds of lines) before the actual
     * error, and the episode column only keeps 500 chars — so report the tail.
     */
    private static String tail(CharSequence output) {
        String text = output.toString().trim();
        if (text.isEmpty()) {
            return "ffmpeg không in ra output nào";
        }
        String[] lines = text.split("\\R");
        int from = Math.max(0, lines.length - 8);
        String joined = String.join(" | ", Arrays.copyOfRange(lines, from, lines.length));
        return joined.length() > 400 ? joined.substring(joined.length() - 400) : joined;
    }
}
