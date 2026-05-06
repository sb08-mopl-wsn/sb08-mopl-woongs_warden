package com.mopl.domain.entity;

import com.mopl.domain.global.base.BaseEntity;
import com.mopl.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationLevel level;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Builder
    public Notification(User user, String title, String content, NotificationLevel level) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.level = level;
        this.isRead = false;
    }

    public void read() {
        this.isRead = true;
    }
}
