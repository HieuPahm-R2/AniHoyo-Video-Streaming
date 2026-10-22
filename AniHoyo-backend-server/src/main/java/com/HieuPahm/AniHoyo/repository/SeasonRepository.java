package com.HieuPahm.AniHoyo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.HieuPahm.AniHoyo.model.entities.Film;
import com.HieuPahm.AniHoyo.model.entities.Season;

public interface SeasonRepository extends JpaRepository<Season, Long>, JpaSpecificationExecutor<Season> {
    List<Season> findByFilm(Film film);

    List<Season> findTop5ByOrderByViewCountDesc();

    @Modifying
    @Query("update Season s set s.viewCount = coalesce(s.viewCount, 0) + :delta where s.id = :seasonId")
    int addViewCount(@Param("seasonId") Long seasonId, @Param("delta") long delta);
}
