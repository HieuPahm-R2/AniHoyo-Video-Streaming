package com.HieuPahm.AniHoyo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.HieuPahm.AniHoyo.model.entities.Role;

public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {
    boolean existsByName(String name);

    Role findByName(String name);

    /**
     * Same as {@link #findByName(String)} but with the lazy permission list
     * fetched, so callers outside a transaction (the database initializer) can
     * add missing permissions without hitting a LazyInitializationException.
     */
    @Query("select distinct r from Role r left join fetch r.permissions where r.name = :name")
    Optional<Role> findByNameWithPermissions(@Param("name") String name);
}
