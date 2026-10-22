package com.HieuPahm.AniHoyo.services.implement;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.HieuPahm.AniHoyo.model.dtos.PaginationResultDTO;
import com.HieuPahm.AniHoyo.model.dtos.SeasonDTO;
import com.HieuPahm.AniHoyo.model.entities.Episode;
import com.HieuPahm.AniHoyo.model.entities.Film;
import com.HieuPahm.AniHoyo.model.entities.Season;
import com.HieuPahm.AniHoyo.repository.EpisodeRepository;
import com.HieuPahm.AniHoyo.repository.FilmRepository;
import com.HieuPahm.AniHoyo.repository.SeasonRepository;
import com.HieuPahm.AniHoyo.services.ISeasonService;
import com.HieuPahm.AniHoyo.utils.error.BadActionException;
import com.turkraft.springfilter.builder.FilterBuilder;
import com.turkraft.springfilter.converter.FilterSpecification;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import com.turkraft.springfilter.parser.node.FilterNode;

@Service
public class SeasonService implements ISeasonService {
    private static final String VIEW_DEDUPLICATION_PREFIX = "view:dedupe:";
    private static final String PENDING_VIEW_PREFIX = "view:pending:episode:";

    @Autowired
    FilterBuilder fb;
    @Autowired
    private FilterParser filterParser;
    @Autowired
    private FilterSpecificationConverter filterSpecificationConverter;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private CacheManager cacheManager;
    @Value("${anihoyo.views.deduplication-hours:24}")
    private long viewDeduplicationHours;

    private final ModelMapper modelMapper;
    private final SeasonRepository seasonRepository;
    private final FilmRepository filmRepository;
    private final EpisodeRepository episodeRepository;

    public SeasonService(ModelMapper modelMapper, SeasonRepository seasonRepository,
            FilmRepository filmRepository, EpisodeRepository episodeRepository) {
        this.modelMapper = modelMapper;
        this.seasonRepository = seasonRepository;
        this.filmRepository = filmRepository;
        this.episodeRepository = episodeRepository;
    }

    @Override
    public SeasonDTO insert(SeasonDTO dto) {
        if (dto.getFilm() != null) {
            // Leaving the transient Film in place used to end as a raw MySQL foreign key
            // violation ("Cannot add or update a child row"), which says nothing about the
            // real problem: the film id does not exist.
            Film film = this.filmRepository.findById(dto.getFilm().getId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Không tìm thấy film với id=" + dto.getFilm().getId()));
            dto.setFilm(film);
        }
        return modelMapper.map(
                this.seasonRepository.save(modelMapper.map(dto, Season.class)), SeasonDTO.class);
    }

    @Override
    @Cacheable(value = "seasons", key = "#id")
    public SeasonDTO getById(Long id) {
        return this.modelMapper.map(
                this.seasonRepository.findById(id).orElseThrow(
                        () -> new NoSuchElementException("Not Found")),
                SeasonDTO.class);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "seasons", key = "#dto.id"),
            @CacheEvict(value = "relatedSeasons", allEntries = true)
    })
    public SeasonDTO update(SeasonDTO dto) throws BadActionException {
        Optional<Season> check = this.seasonRepository.findById(dto.getId());
        if (check.isEmpty()) {
            throw new BadActionException("Not Found");
        }
        Season season = check.get();
        season.setSeasonName(dto.getSeasonName());
        season.setReleaseYear(dto.getReleaseYear());
        return modelMapper.map(
                this.seasonRepository.save(season), SeasonDTO.class);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "seasons", key = "#id"),
            @CacheEvict(value = "relatedSeasons", allEntries = true),
            @CacheEvict(value = "topSeasons", allEntries = true)
    })
    public void delete(Long id) {
        this.seasonRepository.deleteById(id);
    }

    public PaginationResultDTO fetchSeasonsByFilm(Long filmId, Pageable pageable) {
        FilterNode node = filterParser.parse("film.id=" + filmId);
        FilterSpecification<Season> spec = filterSpecificationConverter.convert(node);
        Page<Season> pageCheck = this.seasonRepository.findAll(spec, pageable);

        PaginationResultDTO ans = new PaginationResultDTO();
        PaginationResultDTO.Meta mt = new PaginationResultDTO.Meta();

        mt.setPage(pageCheck.getNumber() + 1);
        mt.setPageSize(pageCheck.getSize());
        mt.setTotal(pageCheck.getTotalElements());
        mt.setPages(pageCheck.getTotalPages());
        ans.setMeta(mt);

        List<SeasonDTO> res = pageCheck.getContent().stream()
                .map(item -> modelMapper.map(item, SeasonDTO.class))
                .collect(Collectors.toList());
        ans.setResult(res);
        return ans;
    }

    public PaginationResultDTO fetchAll(Specification<Season> spec, Pageable pageable) {
        Page<Season> page = this.seasonRepository.findAll(spec, pageable);
        PaginationResultDTO rs = new PaginationResultDTO();
        PaginationResultDTO.Meta mt = new PaginationResultDTO.Meta();

        mt.setPage(page.getNumber() + 1);
        mt.setPageSize(page.getSize());
        mt.setPages(page.getTotalPages());
        mt.setTotal(page.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(page.getContent());
        return rs;
    }

    @Cacheable(value = "relatedSeasons", key = "#seasonId")
    public List<SeasonDTO> getRelatedSeasons(Long seasonId) {
        Season season = this.seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NoSuchElementException("Season Not Found"));
        Film film = season.getFilm();
        if (film == null) return Collections.emptyList();
        return this.seasonRepository.findByFilm(film).stream()
                .map(s -> modelMapper.map(s, SeasonDTO.class))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "topSeasons", key = "'top5'")
    public List<SeasonDTO> getTop5SeasonsByViews() {
        List<Season> topSeasons = seasonRepository.findTop5ByOrderByViewCountDesc();
        return topSeasons.stream()
                .map(season -> modelMapper.map(season, SeasonDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Counts a qualifying play once for a viewer within the configured window.
     * The request touches Redis only; MySQL is updated by flushPendingViewCounts.
     */
    public void increaseViewOnce(Long episodeId, String viewerId) {
        String viewKey = VIEW_DEDUPLICATION_PREFIX + episodeId + ":" + viewerId;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(
                viewKey, "1", Duration.ofHours(viewDeduplicationHours));
        if (Boolean.FALSE.equals(isNew)) {
            return;
        }

        // One counter per episode keeps INCR atomic while avoiding every-view MySQL writes.
        redisTemplate.opsForValue().increment(PENDING_VIEW_PREFIX + episodeId);
    }

    /**
     * Atomically claims pending Redis counters, aggregates them by season, then applies one
     * SQL increment per affected season. Failed database work is put back into Redis for retry.
     */
    @Transactional
    public void flushPendingViewCounts() {
        Map<String, Long> claimedCounters = new HashMap<>();
        try (Cursor<String> keys = redisTemplate.scan(ScanOptions.scanOptions()
                .match(PENDING_VIEW_PREFIX + "*")
                .count(1_000)
                .build())) {
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = redisTemplate.opsForValue().getAndDelete(key);
                Long delta = asPositiveLong(value);
                if (delta != null) {
                    claimedCounters.put(key, delta);
                }
            }
        }

        if (claimedCounters.isEmpty()) {
            return;
        }

        try {
            Map<Long, Long> viewsBySeason = new HashMap<>();
            for (Map.Entry<String, Long> counter : claimedCounters.entrySet()) {
                Long episodeId = episodeIdFromCounterKey(counter.getKey());
                if (episodeId == null) {
                    continue;
                }
                episodeRepository.findById(episodeId)
                        .map(Episode::getSeason)
                        .map(Season::getId)
                        .ifPresent(seasonId -> viewsBySeason.merge(seasonId, counter.getValue(), Long::sum));
            }

            for (Map.Entry<Long, Long> seasonViews : viewsBySeason.entrySet()) {
                seasonRepository.addViewCount(seasonViews.getKey(), seasonViews.getValue());
                evictViewCaches(seasonViews.getKey());
            }
        } catch (RuntimeException exception) {
            // GETDEL prevents a read/delete race. Re-adding the claimed delta preserves it for
            // the next scheduled attempt, including increments received while this flush ran.
            claimedCounters.forEach((key, delta) -> redisTemplate.opsForValue().increment(key, delta));
            throw exception;
        }
    }

    private Long asPositiveLong(Object value) {
        if (value == null) return null;
        try {
            long parsed = Long.parseLong(value.toString());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long episodeIdFromCounterKey(String key) {
        try {
            return Long.valueOf(key.substring(PENDING_VIEW_PREFIX.length()));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void evictViewCaches(Long seasonId) {
        Optional.ofNullable(cacheManager.getCache("seasons")).ifPresent(cache -> cache.evict(seasonId));
        Optional.ofNullable(cacheManager.getCache("relatedSeasons")).ifPresent(cache -> cache.clear());
        Optional.ofNullable(cacheManager.getCache("topSeasons")).ifPresent(cache -> cache.clear());
    }
}
