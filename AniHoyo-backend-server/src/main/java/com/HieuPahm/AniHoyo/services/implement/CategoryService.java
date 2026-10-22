package com.HieuPahm.AniHoyo.services.implement;

import java.util.NoSuchElementException;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.HieuPahm.AniHoyo.model.dtos.CategoryDTO;
import com.HieuPahm.AniHoyo.model.dtos.PaginationResultDTO;
import com.HieuPahm.AniHoyo.model.entities.Category;
import com.HieuPahm.AniHoyo.model.entities.Film;
import com.HieuPahm.AniHoyo.repository.CategoryRepository;
import com.HieuPahm.AniHoyo.repository.FilmRepository;
import com.HieuPahm.AniHoyo.services.ICategoryService;

@Service
public class CategoryService implements ICategoryService {
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final FilmRepository filmRepository;

    public CategoryService(CategoryRepository categoryRepository, ModelMapper modelMapper,
            FilmRepository filmRepository) {
        this.categoryRepository = categoryRepository;
        this.modelMapper = modelMapper;
        this.filmRepository = filmRepository;
    }

    @Override
    public CategoryDTO insert(CategoryDTO dto) {
        return modelMapper.map(categoryRepository.save(modelMapper.map(dto, Category.class)), CategoryDTO.class);
    }

    @Override
    @Cacheable(value = "categories", key = "#id")
    public CategoryDTO getById(Long id) {
        return modelMapper.map(categoryRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("Not Found")), CategoryDTO.class);
    }

    @Override
    @CacheEvict(value = "categories", key = "#dto.id")
    public CategoryDTO update(CategoryDTO dto) {
        return modelMapper.map(categoryRepository.save(modelMapper.map(dto, Category.class)), CategoryDTO.class);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "categories", key = "#id"),
            @CacheEvict(value = "films", allEntries = true)
    })
    public void delete(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Not Found"));
        Set<Film> listFilms = category.getFilms();
        listFilms.forEach(item -> item.getCategories().remove(category));
        filmRepository.saveAll(listFilms);
        categoryRepository.deleteById(id);
    }

    @Override
    public PaginationResultDTO getAll(Specification<Category> spec, Pageable pageable) {
        Page<Category> pageCheck = this.categoryRepository.findAll(spec, pageable);
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

}
