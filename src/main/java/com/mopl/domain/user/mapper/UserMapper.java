package com.mopl.domain.user.mapper;

import com.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class UserMapper {
    @Autowired
    protected JwtRegistry jwtRegistry;

    @Mapping(target = "online", expression = "java(checkOnlineStatus(user))")
    public abstract UserDto toDto(User user);

    protected Boolean checkOnlineStatus(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }

        return jwtRegistry.hasActiveJwtInformationByUserId(user.getId());
    }
}