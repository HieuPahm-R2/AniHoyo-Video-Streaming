package com.HieuPahm.AniHoyo.services.implement;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.HieuPahm.AniHoyo.model.dtos.FilmDTO;
import com.HieuPahm.AniHoyo.model.dtos.PaginationResultDTO;
import com.HieuPahm.AniHoyo.model.entities.Category;
import com.HieuPahm.AniHoyo.model.entities.Tag;
import com.HieuPahm.AniHoyo.model.entities.Film;
import com.HieuPahm.AniHoyo.model.entities.Season;
import com.HieuPahm.AniHoyo.repository.CategoryRepository;
import com.HieuPahm.AniHoyo.repository.EpisodeRepository;
import com.HieuPahm.AniHoyo.repository.FilmRepository;
import com.HieuPahm.AniHoyo.repository.SeasonRepository;
import com.HieuPahm.AniHoyo.repository.TagRepository;
import com.HieuPahm.AniHoyo.services.IFilmService;

@Service
public class FilmService implements IFilmService {
    private final FilmRepository filmRepository;
    private final ModelMapper modelMapper;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    public FilmService(FilmRepository filmRepository, ModelMapper modelMapper, EpisodeRepository episodeRepository,
            CategoryRepository categoryRepository, TagRepository tagRepository, SeasonRepository seasonRepository) {
        this.filmRepository = filmRepository;
        this.modelMapper = modelMapper;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
    }

    @Override
    public FilmDTO insert(FilmDTO dto) {
        Film film = modelMapper.map(dto, Film.class);
        if (dto.getCategories() != null) {
            List<Long> reqCategory = dto.getCategories().stream().map(item -> item.getId())
                    .collect(Collectors.toList());
            Set<Category> mainCategory = this.categoryRepository.findByIdIn(reqCategory);
            film.setCategories(mainCategory);
        }
        if (dto.getTags() != null) {
            List<Long> reqTag = dto.getTags().stream().map(item -> item.getId()).collect(Collectors.toList());
            Set<Tag> mainTag = this.tagRepository.findByIdIn(reqTag);
            film.setTags(mainTag);
        }
        return modelMapper.map(
                filmRepository.save(film), FilmDTO.class);
    }

    @Override
    @Cacheable(value = "films", key = "#id")
    public FilmDTO getById(Long id) {
        return modelMapper.map(
                filmRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Not Found")), FilmDTO.class);
    }

    @Override
    @CacheEvict(value = "films", key = "#dto.id")
    public FilmDTO update(FilmDTO dto) {
        Film film = modelMapper.map(dto, Film.class);
        if (dto.getCategories() != null) {
            List<Long> reqCategory = dto.getCategories().stream().map(item -> item.getId())
                    .collect(Collectors.toList());
            Set<Category> mainCategory = this.categoryRepository.findByIdIn(reqCategory);
            film.setCategories(mainCategory);
        }
        if (dto.getTags() != null) {
            List<Long> reqTag = dto.getTags().stream().map(item -> item.getId()).collect(Collectors.toList());
            Set<Tag> mainTag = this.tagRepository.findByIdIn(reqTag);
            film.setTags(mainTag);
        }
        return modelMapper.map(filmRepository.save(film), FilmDTO.class);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "films", key = "#id"),
            @CacheEvict(value = "relatedSeasons", allEntries = true),
            @CacheEvict(value = "topSeasons", allEntries = true)
    })
    public void delete(Long id) {
        Optional<Film> opt = this.filmRepository.findById(id);
        if (opt.isPresent()) {
            List<Season> seasons = this.seasonRepository.findByFilm(opt.get());
            seasons.forEach(item -> this.episodeRepository.deleteAll(this.episodeRepository.findBySeason(item)));
            this.seasonRepository.deleteAll(seasons);
        }
        filmRepository.deleteById(id);
    }

    @Override
    public PaginationResultDTO getAll(Specification<Film> spec, Pageable pageable) {
        Page<Film> pageCheck = this.filmRepository.findAll(spec, pageable);
        PaginationResultDTO res = new PaginationResultDTO();
        PaginationResultDTO.Meta mt = new PaginationResultDTO.Meta();
        mt.setPage(pageCheck.getNumber() + 1);
        mt.setPageSize(pageCheck.getSize());
        mt.setPages(pageCheck.getTotalPages());
        mt.setTotal(pageCheck.getTotalElements());
        res.setMeta(mt);
        res.setResult(pageCheck.getContent());
        return res;
    }

    @Override
    public FilmDTO getFilmBySeasonId(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFilmBySeasonId'");
    }

}
