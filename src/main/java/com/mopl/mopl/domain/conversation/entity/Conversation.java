package com.mopl.mopl.domain.conversation.entity;

import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.global.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conversations_participant_pair",
                        columnNames = {"participant_pair_key"}
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

    // 양방향 중복 방지용 정규화키
    @Column(name = "participant_pair_key", nullable = false, updatable = false, length = 100)
    private String participantPairKey;

    @Builder
    public Conversation(User sender, User receiver) {
        this.sender = sender;
        this.receiver = receiver;
        this.participantPairKey = buildPairKey(sender.getId(), receiver.getId());
    }

    public void updateUnreadStatus(boolean hasUnread) {
        this.hasUnread = hasUnread;
    }

    public static String buildPairKey(UUID id1, UUID id2) {
        return (id1.compareTo(id2) < 0) ? id1 + ":" + id2 : id2 + ":" + id1;
    }
}
