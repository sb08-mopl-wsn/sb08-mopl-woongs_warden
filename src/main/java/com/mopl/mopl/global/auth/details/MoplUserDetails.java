package com.mopl.mopl.global.auth.details;

import com.mopl.mopl.domain.user.dto.UserDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public class MoplUserDetails implements UserDetails, OAuth2User {
    private final UserDto userDto;
    private final String password;
    private Map<String, Object> attributes; // 소셜 로그인 정보용

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

    @Override
    public String getName() {
        return userDto.name();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !userDto.locked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoplUserDetails that)) return false;
        return Objects.equals(userDto.email(), that.userDto.email());}

    @Override
    public int hashCode() {
        return Objects.hashCode(userDto.email());
    }
}
