package com.mopl.mopl.domain.watchingSession.mapper;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.mapper.ContentMapper;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import com.mopl.mopl.domain.watchingSession.dto.response.ContentChatDto;
import com.mopl.mopl.domain.watchingSession.dto.response.WatchingSessionDto;
import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring",
    uses = {UserMapper.class, ContentMapper.class}
)

public interface WatchingSessionMapper {

    @Mapping(target = "watcher", source = "user")
    @Mapping(target = "content", source = "content")
    WatchingSessionDto toDto(WatchingSession watchingSession);

    @Mapping(target = "id", source = "sessionId")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "watcher", source = "user")
    @Mapping(target = "content", source = "content")
    WatchingSessionDto toDto(UUID sessionId, Instant createdAt, Content content, User user);

    ContentChatDto toChatDto(UserSummary sender, String content);
}
