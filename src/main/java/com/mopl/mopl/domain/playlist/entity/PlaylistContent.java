package com.mopl.mopl.domain.playlist.entity;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.global.base.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "playlist_contents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_playlist_contents",
                columnNames = {"playlist_id", "content_id"}
        )
)
@Entity
public class PlaylistContent extends BaseEntity
{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false, referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_playlist_contents_playlist_id"))
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false, referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_playlist_contents_content_id"))
    private Content content;

    public PlaylistContent(Playlist playlist, Content content) {
        this.playlist = playlist;
        this.content = content;
    }
}
