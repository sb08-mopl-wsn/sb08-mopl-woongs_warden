package com.mopl.mopl.domain.review.dto.response;

import java.util.UUID;

public record ReviewDto(
    UUID id,            // 리뷰 ID
    UUID contentId,     // 콘텐츠 ID
    UUID userId,        // 작성자 정보  //Todo usersummary 완성되면 바꿔주기 UserSummary author
    String text,        // 리뷰 내용
    double rating       // 평점 (0.0 ~ 5.0)
) {

}
