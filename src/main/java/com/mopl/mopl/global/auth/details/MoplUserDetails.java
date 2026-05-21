package com.mopl.mopl.global.auth.details;

import com.mopl.mopl.domain.user.dto.UserDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class MoplUserDetails implements UserDetails, OAuth2User {
    private final UserDto userDto;
    private final String password;
    private final Map<String, Object> attributes;

    public MoplUserDetails(UserDto userDto, String password) {
        this(userDto, password, Collections.emptyMap());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + userDto.role().name())
        );
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userDto.email();
    }

    public String getName() {
        return userDto.name();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !userDto.locked();
    }
}