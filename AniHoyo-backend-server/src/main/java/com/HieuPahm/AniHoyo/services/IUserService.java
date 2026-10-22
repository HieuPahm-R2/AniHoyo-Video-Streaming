package com.HieuPahm.AniHoyo.services;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.HieuPahm.AniHoyo.model.dtos.PaginationResultDTO;
import com.HieuPahm.AniHoyo.model.dtos.auth.UpdateUserDTO;
import com.HieuPahm.AniHoyo.model.dtos.auth.UserDTO;
import com.HieuPahm.AniHoyo.model.entities.User;
import com.HieuPahm.AniHoyo.utils.error.BadActionException;

public interface IUserService {
    public UserDTO create(User data) throws BadActionException;

    public UserDTO getInfo(Long id);

    public PaginationResultDTO getAll(Specification<User> spec, Pageable pageable);

    public void delete(Long id) throws BadActionException;

    public UpdateUserDTO update(User data) throws BadActionException;

    public User handleGetUserByUsername(String username);

    public User getUserWithPermissions(String email);
}
