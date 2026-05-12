package com.mopl.mopl.domain.user.mapper;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.dto.request.UserCreateRequest;
import com.mopl.mopl.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;

@Mapper(componentModel = "spring")
public abstract class UserMapper {
//    @Value()   // 나중에 이미지 기본경로 추가해주기, yaml에서 불러와야할 듯
    protected String imageBaseUrl = null; // 임시로 null

    @Mapping(target = "profileImageUrl",  source = "profileImageKey", qualifiedByName = "buildProfileImageUrl")
    public abstract UserDto toDto(User user);

    @Named("buildProfileImageUrl")
    protected String buildImageUrl(String profileImageKey) {
        if (profileImageKey == null || profileImageKey.isEmpty()) {
            return null;  // 기본 이미지 있다면 그걸로 바꾸기
        }
        String baseUrl = (imageBaseUrl == null) ? "" : imageBaseUrl;
        return imageBaseUrl + profileImageKey;
    }

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "profileImageUrl", source = "profileImageKey", qualifiedByName = "buildProfileImageUrl")
    public abstract UserSummary toUserSummary(User user);
}