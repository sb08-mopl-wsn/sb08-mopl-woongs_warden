package com.mopl.mopl.domain.review.mapper;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.mapper.UserMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ReviewMapper {

  @BeanMapping(builder = @Builder(disableBuilder = true))
  @Mapping(source = "request.text", target = "description")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Review toEntity(ReviewCreateRequest request, User user, Content content);

  @Mapping(source = "description", target = "text")
  @Mapping(source = "user", target = "author")
  @Mapping(source = "content.id", target = "contentId")
  ReviewDto toDto(Review review);
}
