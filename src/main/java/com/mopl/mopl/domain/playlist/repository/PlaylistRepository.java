package com.mopl.mopl.domain.playlist.repository;

import com.mopl.mopl.domain.playlist.entity.Playlist;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID>, PlaylistRepositoryCustom {

  @Query("SELECT p FROM Playlist p JOIN FETCH p.user WHERE p.id = :id")
  Optional<Playlist> findByIdWithUser(@Param("id") UUID id);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Playlist p SET p.contentCount = p.contentCount + 1 WHERE p.id = :id")
  int increaseContentCount(@Param("id") UUID id);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Playlist p SET p.contentCount = p.contentCount - 1 WHERE p.id = :id AND p.contentCount > 0")
  int decreaseContentCount(@Param("id") UUID id);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Playlist p SET p.subscriberCount = p.subscriberCount + 1 WHERE p.id = :id")
  int increaseSubscriberCount(@Param("id") UUID id);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE Playlist p SET p.subscriberCount = p.subscriberCount - 1 WHERE p.id = :id AND p.subscriberCount > 0")
  int decreaseSubscriberCount(@Param("id") UUID id);
}