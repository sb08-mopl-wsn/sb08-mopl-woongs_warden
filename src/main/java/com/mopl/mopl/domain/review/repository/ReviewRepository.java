package com.mopl.mopl.domain.review.repository;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface ReviewRepository extends JpaRepository<Review, UUID>, ReviewRepositoryCustom {

  // 수정,삭제,조회
  Optional<Review> findByUserAndContent(User user, Content content);

  //
  interface ReviewStats {
    Long getReviewCount();
    Double getAverageRating();
  }

  @Query("SELECT COUNT(r) AS reviewCount, COALESCE(AVG(r.rating), 0.0) AS averageRating " +
      "FROM Review r WHERE r.content.id = :contentId")
  ReviewStats getReviewStats(@Param("contentId") UUID contentId);

}