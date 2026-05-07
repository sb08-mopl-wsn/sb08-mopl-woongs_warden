package com.mopl.mopl.entity;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.global.base.BaseUpdatableEntity;
import com.mopl.mopl.domain.user.entity.User;
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
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reviews_user_content", columnNames = {"user_id", "content_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseUpdatableEntity {

    @Column(nullable = false)
    private Double rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Builder

    public Review(Double rating, String description, User user, Content content) {
        this.rating = rating;
        this.description = description;
        this.user = user;
        this.content = content;
    }
}
