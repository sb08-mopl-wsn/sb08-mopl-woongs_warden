package com.mopl.mopl.domain.dm.entity;

import com.mopl.mopl.domain.conversation.entity.Conversation;
import com.mopl.mopl.global.base.BaseEntity;
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
@Table(name = "direct_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // 이 특정 메시지를 보낸 유저

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public DirectMessage(Conversation conversation, User sender, String content) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
    }
}
