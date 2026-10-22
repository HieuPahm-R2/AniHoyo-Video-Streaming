package com.HieuPahm.AniHoyo.config;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import org.springframework.security.core.userdetails.User;
import com.HieuPahm.AniHoyo.services.IUserService;

@Component("userDetailsService")
public class UserDetailsConfig implements UserDetailsService {
    private final IUserService userService;

    public UserDetailsConfig(IUserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.HieuPahm.AniHoyo.model.entities.User user = this.userService.handleGetUserByUsername(username);
        return new User(user.getEmail(), user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE-USER")));
    }
}
