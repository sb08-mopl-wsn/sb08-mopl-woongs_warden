package com.mopl.mopl.domain.review.repository;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ReviewRepository extends JpaRepository<Review, UUID>, ReviewRepositoryCustom {

  // 수정,삭제,조회
  Optional<Review> findByUserAndContent(User user, Content content);

  @Query("SELECT r.content FROM Review r WHERE r.user.id = :userId AND r.rating >= :minRating")
  List<Content> findHighRatedContents(@Param("userId") UUID userId, @Param("minRating") double minRating);
}