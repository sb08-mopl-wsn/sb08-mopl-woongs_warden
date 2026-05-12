package com.mopl.mopl.domain.playlist.entity;

import com.mopl.mopl.global.base.BaseUpdatableEntity;
import com.mopl.mopl.domain.user.entity.User;
import jakarta.persistence.Column;
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
@Table(name = "playlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist extends BaseUpdatableEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long subscriberCount;

    @Column(nullable = false)
    private Long contentCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public Playlist(User user, String description, String title) {
        this.user = user;
        this.contentCount = 0L;
        this.subscriberCount = 0L;
        this.description = description;
        this.title = title;
    }

    public void update(String title, String description) {

        //플리 제목이 null이 아니고, 공백만으로 이루어져 있지 않을 때만 업데이트
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        //플리 설명이 null이 아닐 때만 업데이트
        if (description != null) {
            this.description = description;
        }
    }
}
