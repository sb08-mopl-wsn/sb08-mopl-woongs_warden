package com.mopl.mopl.domain.review.repository;

import com.mopl.mopl.domain.review.entity.Review;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;


public interface ReviewRepository extends JpaRepository<Review, UUID> {

  // 수정,삭제,조회
  Optional<Review> findByUserIdAndContentId(UUID userId, UUID contentId);
}