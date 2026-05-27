package com.mopl.mopl.domain.user.mapper;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import com.mopl.mopl.infrastructure.s3.ImageUrlConverter;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Mapper(componentModel = "spring", uses = ImageUrlConverter.class)
public abstract class UserMapper {
    @Mapping(target = "profileImageUrl", source = "profileImageKey", qualifiedByName = "toThumbnailUrl")
    public abstract UserDto toDto(User user);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "profileImageUrl", source = "profileImageKey", qualifiedByName = "toThumbnailUrl")
    public abstract UserSummary toUserSummary(User user);
}