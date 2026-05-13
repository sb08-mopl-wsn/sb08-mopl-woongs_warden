package com.mopl.mopl.domain.dm.entity;

import com.mopl.mopl.domain.conversation.entity.Conversation;
import com.mopl.mopl.global.base.BaseEntity;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;
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

        // 객체 null 검증
        if (conversation == null) {
            throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "대화방 정보가 누락되었습니다.");
        }
        if (sender == null) {
            throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "발신자 정보가 누락되었습니다.");
        }

        // 비즈니스 룰 검증
        if (content == null || content.isBlank()) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "메시지 내용은 비어있을 수 없습니다.");
        }
        if (content.length() > 2000) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "메시지는 최대 2000자까지 입력 가능합니다.");
        }

        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
    }
}
