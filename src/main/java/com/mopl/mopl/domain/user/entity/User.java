package com.mopl.mopl.domain.user.entity;

import com.mopl.mopl.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User  extends BaseEntity {
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

    /**
     * 임시 비밀번호 생성시 원본을 저장<br>
     * 임시 비밀번호가 만료되면<br>
     * 이걸로 원래 비밀번호로 롤백시킴<br>
     * 비번을 바꾼다하면<br>
     * 이전 비밀번호로 남겨서 나중에<br>
     * 사용했던 비번입니다로 응용가능*/
    @Column(length = 255, nullable = true)
    private String temporaryPassword;

    @Column(nullable = false)
    private boolean init_password;

    @Builder(
            builderMethodName = "builder",
            builderClassName = "UserBuilder"
    )
    public User(String name, String email, String password , Social socialType, String socialId) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = Role.USER;
        this.isLocked = false;
        this.profileImageKey = null;
        this.socialType = socialType != null ? socialType : null;
        this.socialId = socialId != null ? socialId:null;
        this.init_password = false;
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
        this.socialType =   null;
        this.socialId = null;
    }

    public User updateName(String newName) {
        this.name = newName;
        return this;
    }

    public User updatePassword(String encryptedPassword) {
        this.password = encryptedPassword;
        this.init_password = false;
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

    public void updateTemporaryPassword(String initPassowrd,String originPassword, Instant expiredAt) {
        this.temporaryPassword = originPassword;
        this.password = initPassowrd;
        this.temporaryPasswordExpiredAt = expiredAt;
        this.init_password = true;
    }
}
