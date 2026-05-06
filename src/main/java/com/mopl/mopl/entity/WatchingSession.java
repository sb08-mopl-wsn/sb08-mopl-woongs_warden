package com.mopl.mopl.entity;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.global.base.BaseEntity;
import com.mopl.mopl.user.entity.User;
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

@Getter
@Entity
@Table(
        name = "watching_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_watching_sessions_content_watcher",
                        columnNames = {"content_id", "watcher_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchingSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "content_id",
            nullable = false
    )
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Builder
    public WatchingSession(Content content, User user) {
        this.content = content;
        this.user = user;
    }
}
