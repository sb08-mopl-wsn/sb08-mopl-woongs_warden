package com.mopl.mopl.domain.content.mapper;

import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.entity.Content;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContentMapper
{
    ContentDto toContentDto(Content content);
    List<ContentDto> toContentDtos(List<Content> contents);
}
