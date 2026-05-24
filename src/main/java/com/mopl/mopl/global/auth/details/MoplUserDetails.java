package com.mopl.mopl.global.auth.details;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(value = {"authorities"},ignoreUnknown = true)
public class MoplUserDetails implements UserDetails, OAuth2User {
    private final UserDto userDto;
    private final String password;
    private final Map<String, Object> attributes;

    @JsonCreator
    public MoplUserDetails(
            @JsonProperty("userDto") UserDto userDto,
            @JsonProperty("password") String password,
            @JsonProperty("attributes") Map<String, Object> attributes
    ) {
        this.userDto = userDto;
        this.password = password;
        this.attributes = attributes == null ? Collections.emptyMap() : attributes;
    }

    @Override
    @JsonIgnore
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