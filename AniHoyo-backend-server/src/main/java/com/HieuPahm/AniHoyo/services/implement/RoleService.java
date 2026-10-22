package com.HieuPahm.AniHoyo.services.implement;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.HieuPahm.AniHoyo.model.dtos.PaginationResultDTO;
import com.HieuPahm.AniHoyo.model.entities.Permission;
import com.HieuPahm.AniHoyo.model.entities.Role;
import com.HieuPahm.AniHoyo.repository.PermissionRepository;
import com.HieuPahm.AniHoyo.repository.RoleRepository;
import com.HieuPahm.AniHoyo.services.IRoleService;
import com.HieuPahm.AniHoyo.utils.error.BadActionException;

@Service
public class RoleService implements IRoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public Role create(Role data) {
        List<Long> pers = data.getPermissions().stream().map(item -> item.getId())
                .collect(Collectors.toList());
        List<Permission> allPers = this.permissionRepository.findByIdIn(pers);
        data.setPermissions(allPers);
        return this.roleRepository.save(data);
    }

    @Override
    @CacheEvict(value = "roles", key = "#data.id")
    public Role update(Role data) throws BadActionException {
        if (Long.valueOf(data.getId()) == null || this.roleRepository.findById(data.getId()).isEmpty()) {
            throw new BadActionException("Không tìm thấy bất kỳ thông tin nào, kiểm tra lại?");
        } else {
            Optional<Role> rOptional = this.roleRepository.findById(data.getId());
            if (rOptional.isPresent()) {
                List<Long> allItems = data.getPermissions().stream().map(item -> item.getId())
                        .collect(Collectors.toList());
                List<Permission> allPermissions = this.permissionRepository.findByIdIn(allItems);
                data.setPermissions(allPermissions);
                data.setCreatedBy(rOptional.get().getCreatedBy());
                data.setCreatedTime(rOptional.get().getCreatedTime());
                return this.roleRepository.save(data);
            }
            return null;
        }
    }

    @Override
    @Cacheable(value = "roles", key = "#id")
    public Role fetchById(Long id) throws BadActionException {
        Optional<Role> check = this.roleRepository.findById(id);
        if (!check.isPresent()) {
            throw new BadActionException("Không tìm tháy dữ liệu!");
        }
        return check.get();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "roles", key = "#id"),
            // Evict all userByEmail entries since roles affect auth decisions
            @CacheEvict(value = "userByEmail", allEntries = true)
    })
    public void delete(Long id) throws BadActionException {
        if (this.roleRepository.findById(id).isEmpty()) {
            throw new BadActionException("Không tìm thấy dữ liệu! (Có thể do ID không hợp lệ)");
        }
        this.roleRepository.deleteById(id);
    }

    @Override
    public PaginationResultDTO fetchAll(Specification<Role> spec, Pageable pageable) {
        Page<Role> page = this.roleRepository.findAll(spec, pageable);

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

}
