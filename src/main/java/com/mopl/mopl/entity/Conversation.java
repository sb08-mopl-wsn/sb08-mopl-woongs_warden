package com.mopl.mopl.entity;

import com.mopl.mopl.global.base.BaseUpdatableEntity;
import com.mopl.mopl.user.entity.User;
import jakarta.persistence.Column;
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
        name = "conversations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conversations_sender_receiver",
                        columnNames = {"sender_id", "receiver_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseUpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // 대화를 처음 시작한 유저

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver; // 대화 상대방 유저

    @Column(name = "has_unread", nullable = false)
    private boolean hasUnread = false;

    @Builder
    public Conversation(User sender, User receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    public void updateUnreadStatus(boolean hasUnread) {
        this.hasUnread = hasUnread;
    }
}
