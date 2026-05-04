package com.mopl.mopl.entity;

import com.mopl.mopl.entity.base.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlist_subscriptions")
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
