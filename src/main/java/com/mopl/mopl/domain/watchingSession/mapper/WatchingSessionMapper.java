package com.mopl.mopl.domain.watchingSession.mapper;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.watchingSession.dto.WatchingSessionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface WatchingSessionMapper {

    @Mapping(target = "id", source = "sessionId")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "watcher", source = "user")
    @Mapping(target = "content", source = "content")
    WatchingSessionDto toDto(UUID sessionId, Instant createdAt, Content content, User user);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "profileImageUrl", source = "profileImageKey")
    WatchingSessionDto.WatcherDto toWatcherDto(User user);

    @Mapping(target = "thumbnailUrl", source = "thumbnailKey")
    @Mapping(target = "averageRating", source = "avgRating")
    @Mapping(target = "type", source = "contentType")
    WatchingSessionDto.ContentInfoDto toContentInfoDto(Content content);
}
