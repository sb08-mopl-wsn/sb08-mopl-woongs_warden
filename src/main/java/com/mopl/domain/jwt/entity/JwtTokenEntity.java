package com.mopl.domain.jwt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_jwt_token")
public class JwtTokenEntity {
    // 토큰 고유 식별자(jti)
    @Id
    @Column(name = "jti", length = 64)
    private String jti;

    // 사용자명(주체)
    @Column(name = "username", nullable = false)
    private String username;

    // 토큰 타입(access | refresh)
    @Column(name = "token_type", nullable = false, length = 16)
    private String tokenType; // access | refresh

    // 발급 시각(UTC)
    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    // 만료 시각(UTC)
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    // 폐기 여부
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    // 회전 시 새 리프레시 토큰의 jti
    @Column(name = "replaced_by", length = 64)
    private String replacedBy;

    public JwtTokenEntity(String jti, String username, String tokenType, OffsetDateTime issuedAt, OffsetDateTime expiresAt) {
        this.jti = jti;
        this.username = username;
        this.tokenType = tokenType;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString() {
        return "JwtTokenEntity{" +
                "jti='" + jti + '\'' +
                ", username='" + username + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", revoked=" + revoked +
                ", replacedBy='" + replacedBy + '\'' +
                '}';
    }
}
