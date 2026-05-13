package com.mopl.mopl.domain.user.mapper;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Mapper(componentModel = "spring")
public abstract class UserMapper {
    @Value("${cloud.aws.s3.cdn-url}")
    protected String imageBaseUrl;

    @Mapping(target = "profileImageUrl", source = "profileImageKey", qualifiedByName = "buildProfileImageUrl")
    public abstract UserDto toDto(User user);

    @Named("buildProfileImageUrl")
    protected String buildImageUrl(String profileImageKey) {
        if (profileImageKey == null || profileImageKey.isEmpty()) {
            return null;  // 기본 이미지 있다면 그걸로 바꾸기
        }

        String baseUrl = imageBaseUrl;

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new BusinessException(GlobalErrorCode.CDN_URL_NOT_FOUND);
        }

        baseUrl = baseUrl.trim();

        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "https://" + baseUrl;
        }

        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedKey = profileImageKey.startsWith("/") ? profileImageKey.substring(1) : profileImageKey;
        return normalizedBaseUrl + "/" + normalizedKey;
    }

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "profileImageUrl", source = "profileImageKey", qualifiedByName = "buildProfileImageUrl")
    public abstract UserSummary toUserSummary(User user);
}