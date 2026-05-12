package com.mopl.mopl.domain.playlist.mapper;

import com.mopl.mopl.domain.content.mapper.ContentMapper;
import com.mopl.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ContentMapper.class})
public interface PlaylistMapper {

  @Mapping(source = "user", target = "owner")
  // TODO: findPlaylistById 구현 시, 이 매핑은 동적으로 처리되도록 수정 필요
  @Mapping(target = "subscribedByMe", constant = "true")
  // TODO: ContentSummary DTO 구현 후, contents 필드 매핑 로직 추가 필요
  PlaylistDto toDto(Playlist playlist);
}
