package com.mopl.mopl.domain.playlist.mapper;

import com.mopl.mopl.domain.content.dto.response.ContentSummary;
import com.mopl.mopl.domain.content.mapper.ContentMapper;
import com.mopl.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ContentMapper.class})
public interface PlaylistMapper {

  @Mapping(source = "playlist.user", target = "owner")
  @Mapping(source = "isSubscribed", target = "subscribedByMe")
  PlaylistDto toDto(Playlist playlist, Boolean isSubscribed, List<ContentSummary> contents);

  @Mapping(source = "user", target = "owner")
  @Mapping(target = "subscribedByMe", constant = "true")
  PlaylistDto toDto(Playlist playlist);
}
