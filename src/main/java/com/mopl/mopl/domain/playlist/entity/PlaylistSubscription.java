package com.mopl.mopl.domain.playlist.entity;

import com.mopl.mopl.global.base.BaseEntity;
import com.mopl.mopl.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "playlist_subscriptions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_playlist_subscriptions",
        columnNames = {"user_id", "playlist_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @Builder
    public PlaylistSubscription(User user, Playlist playlist) {
        this.user = user;
        this.playlist = playlist;
    }
}
