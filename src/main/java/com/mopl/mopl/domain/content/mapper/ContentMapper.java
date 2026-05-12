package com.mopl.mopl.domain.content.mapper;

import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.dto.response.ContentSummary;
import com.mopl.mopl.domain.content.entity.Content;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContentMapper
{
    @Mapping(source = "contentType", target = "type")
    @Mapping(source = "thumbnailKey", target = "thumbnailUrl")
    @Mapping(source = "avgRating", target = "averageRating")
    ContentDto toContentDto(Content content);
    List<ContentDto> toContentDtos(List<Content> contents);

    @Mapping(target = "type", source = "contentType")
    @Mapping(target = "thumbnailUrl", source = "thumbnailKey")
    @Mapping(target = "averageRating", source = "avgRating")
    ContentSummary toContentSummary(Content content);
}
