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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "JSONB", nullable = false)
    private List<String> tags;

    @Column(name = "release_date", nullable = true)
    private Instant releaseDate;

    @Builder
    public Content(String title, String description, ContentType contentType, String thumbnailKey, List<String> tags, Instant releaseDate) {
        this.title = title;
        this.description = description;
        this.contentType = contentType;
        this.thumbnailKey = thumbnailKey;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.releaseDate = releaseDate;
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
}
