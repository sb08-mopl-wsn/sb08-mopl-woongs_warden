package com.mopl.mopl.domain.review.repository;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ReviewRepository extends JpaRepository<Review, UUID>, ReviewRepositoryCustom {

  // 수정,삭제,조회
  Optional<Review> findByUserAndContent(User user, Content content);
}