package com.mopl.mopl.domain.content.entity;

import com.mopl.mopl.global.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "contents")
@Entity
public class Content extends BaseUpdatableEntity
{
    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 50, nullable = false)
    private ContentType contentType;

    @Column(name = "thumbnail_key", length = 255, nullable = false)
    private String thumbnailKey;

    @Column(name = "avg_rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Column(name = "watcher_count", nullable = false)
    private int watcherCount = 0;

    @Column(name = "external_id", length = 50)
    private String externalId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "JSONB", nullable = false)
    private List<String> tags;

    @Column(name = "release_date", nullable = true)
    private Instant releaseDate;

    @Builder
    public Content(String title, String description, ContentType contentType, String thumbnailKey, List<String> tags, Instant releaseDate, String externalId) {
        this.title = title;
        this.description = description;
        this.contentType = contentType;
        this.thumbnailKey = thumbnailKey;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.releaseDate = releaseDate;
        this.externalId = externalId;
    }

    public void update(String title, String description, List<String> tags) {
        if (title != null) {
            this.title = title;
        }

        if (description != null) {
            this.description = description;
        }

        if (tags != null) {
            this.tags = tags;
        }
    }

    public void updateReviewStats(BigDecimal avgRating, int reviewCount) {
        if (avgRating == null) {
            throw new IllegalArgumentException("avgRating은 null이면 안됩니다.");
        }

        if (reviewCount < 0) {
            throw new IllegalArgumentException("reviewCount는 0 이상이어야 합니다.");
        }

        if (avgRating.compareTo(BigDecimal.ZERO) < 0 || avgRating.compareTo(new BigDecimal("5.0")) > 0) {
            throw new IllegalArgumentException("angRating은 0.0과 5.0 사이 값이어야 합니다.");
        }

        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }

    public void updateWatcherCount(int watcherCount) {
        if (watcherCount < 0) {
            throw new IllegalArgumentException("watcherCount는 0 이상이어야 합니다.");
        }
        
        this.watcherCount = watcherCount;
    }
}
