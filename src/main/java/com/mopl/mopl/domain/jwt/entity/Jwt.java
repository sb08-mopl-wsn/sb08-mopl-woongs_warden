package com.mopl.mopl.domain.jwt.entity;

import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "jwt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Jwt extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean isRevoked;

    @Column(length = 255)
    private String refreshToken;

    @Builder
    public Jwt(User user, Instant expiresAt, String refreshToken) {
        this.user = user;
        this.expiresAt = expiresAt;
        this.refreshToken = refreshToken;
        this.isRevoked = false;
    }

    public void revoke() {
        this.isRevoked = true;
    }
}
