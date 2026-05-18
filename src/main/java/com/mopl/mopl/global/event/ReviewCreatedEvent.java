package com.mopl.mopl.global.event;

import java.util.UUID;

/**
 * 리뷰 생성 이벤트
 * @param reviewId 리뷰 ID
 * @param writerId 리뷰 작성자 ID
 * @param writerName 리뷰 작성자 이름
 */
public record ReviewCreatedEvent(
    UUID reviewId,
    UUID writerId,
    String writerName
) {

  // 팩토리 메서드
  public static ReviewCreatedEvent of(UUID reviewId, UUID writerId, String writerName) {
    return new ReviewCreatedEvent(reviewId, writerId, writerName);
  }

}
