package com.mopl.mopl.domain.content.mapper;

import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.dto.response.ContentSummary;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.infrastructure.s3.ImageUrlConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = ImageUrlConverter.class)
public interface ContentMapper
{
    @Mapping(source = "contentType", target = "type")
    @Mapping(source = "thumbnailKey", target = "thumbnailUrl", qualifiedByName = "toThumbnailUrl")
    @Mapping(source = "avgRating", target = "averageRating")
    ContentDto toContentDto(Content content);

    // toContentDto만 작성해도 적용
    List<ContentDto> toContentDtos(List<Content> contents);

    @Mapping(target = "type", source = "contentType")
    @Mapping(source = "thumbnailKey", target = "thumbnailUrl", qualifiedByName = "toThumbnailUrl")
    @Mapping(target = "averageRating", source = "avgRating")
    ContentSummary toContentSummary(Content content);
}
