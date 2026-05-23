package com.mopl.mopl.domain.user.entity;

import com.mopl.mopl.domain.user.exception.UserInvalidSocialInfoException;
import com.mopl.mopl.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_social_type_social_id",
                        columnNames = {"social_type", "social_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 255, nullable = true)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean isLocked;

    @Column(length = 255, nullable = true)
    private String profileImageKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 8, nullable = true)
    private Social socialType;

    @Column(length = 100, nullable = true)
    private String socialId;

    @Column(nullable = true)
    private Instant temporaryPasswordExpiredAt;

    @Column(length = 255, nullable = true)
    private String temporaryPassword;

    @Column(nullable = false)
    private boolean initPassword;

    @Column(nullable = false)
    private int warningCount;

    @Column(nullable = false)
    private boolean isBanned;

    @Column(nullable = false)
    private LocalDateTime bannedAt;

    @Column(nullable = true)
    private LocalDateTime banExpiresAt;

    @Builder(
            builderMethodName = "builder",
            builderClassName = "UserBuilder"
    )
    public User(String name, String email, String password, Social socialType, String socialId) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = Role.USER;
        this.isLocked = false;
        this.profileImageKey = null;
        this.socialType = socialType != null ? socialType : null;
        this.socialId = socialId != null ? socialId : null;
        this.initPassword = false;
        this.warningCount = 0;
        this.isBanned = false;
    }

    @Builder(
            builderMethodName = "adminBuilder",
            builderClassName = "AdminBuilder"
    )
    // 어드민 생성용
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = Role.ADMIN;
        this.isLocked = false;
        this.profileImageKey = null;
        this.socialType = null;
        this.socialId = null;
        this.warningCount = 0;
        this.isBanned = false;
    }

    public User updateName(String newName) {
        this.name = newName;
        return this;
    }

    public User updatePassword(String encryptedPassword) {
        this.password = encryptedPassword;
        this.initPassword = false;
        return this;
    }

    public User updateProfileImage(String newProfileImageKey) {
        this.profileImageKey = newProfileImageKey;
        return this;
    }

    public User updateRole(Role newRole) {
        this.role = newRole;
        return this;
    }

    public User lock() {
        this.isLocked = true;
        return this;
    }

    public User unlock() {
        this.isLocked = false;
        return this;
    }

    public void updateTemporaryPassword(String initPassowrd, String originPassword, Instant expiredAt) {
        this.temporaryPassword = originPassword;
        this.password = initPassowrd;
        this.temporaryPasswordExpiredAt = expiredAt;
        this.initPassword = true;
    }

    public User updateSocialInfo(Social socialType, String socialId) {
        if ((socialType == null) != (socialId == null || socialId.isBlank())) {
            throw new UserInvalidSocialInfoException();
        }

        this.socialType = socialType;
        this.socialId = socialId;
        return this;
    }

    public void increaseWarningCount() {
        this.warningCount++;
        if (this.warningCount % 3 == 0) {
            this.isBanned = true;
            this.bannedAt = LocalDateTime.now();

            int banCount = this.warningCount / 3;

            switch (banCount) {
                case 1 -> this.banExpiresAt = LocalDateTime.now().plusMinutes(1);
                case 2 -> this.banExpiresAt = LocalDateTime.now().plusHours(1);
                default -> {
                    this.isLocked = true;
                    this.banExpiresAt = null;
                }
            }
        }
    }

    public void unBan() {
        this.isBanned = false;
        this.bannedAt = null;
        this.banExpiresAt = null;
    }
}
