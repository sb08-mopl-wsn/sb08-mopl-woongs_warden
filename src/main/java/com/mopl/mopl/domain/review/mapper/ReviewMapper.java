package com.mopl.mopl.domain.review.mapper;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.user.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

  @BeanMapping(builder = @Builder(disableBuilder = true))
  @Mapping(source = "request.text", target = "description")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Review toEntity(ReviewCreateRequest request, User user, Content content);

  @Mapping(source = "description", target = "text")
  //Todo UserSummary완성되면 수정 @Mapping(source = "user", target = "author")
  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "content.id", target = "contentId")
  ReviewDto toDto(Review review);
}
