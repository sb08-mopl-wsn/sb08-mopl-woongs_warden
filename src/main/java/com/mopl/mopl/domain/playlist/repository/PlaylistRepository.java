package com.mopl.mopl.domain.playlist.repository;

import com.mopl.mopl.domain.playlist.entity.Playlist;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID>, PlaylistRepositoryCustom {

  @Query("SELECT p FROM Playlist p JOIN FETCH p.user WHERE p.id = :id")
  Optional<Playlist> findByIdWithUser(@Param("id") UUID id);
}