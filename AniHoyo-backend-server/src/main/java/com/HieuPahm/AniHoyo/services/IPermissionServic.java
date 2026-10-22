package com.HieuPahm.AniHoyo.services;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.HieuPahm.AniHoyo.model.dtos.PaginationResultDTO;
import com.HieuPahm.AniHoyo.model.entities.Permission;
import com.HieuPahm.AniHoyo.utils.error.BadActionException;

public interface IPermissionServic {
    Permission create(Permission data);

    Permission update(Permission data) throws BadActionException;

    Permission getById(Long id) throws BadActionException;

    void delete(Long id) throws BadActionException;

    PaginationResultDTO fetchAll(Specification<Permission> spec, Pageable pageable);

    boolean alreadyExistPermission(Permission data);

    boolean isEqualName(String s);
}
