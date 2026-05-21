package com.mopl.mopl.infrastructure.elasticsearch.document;

import com.mopl.mopl.domain.content.entity.Content;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "contents")
@Setting(settingPath = "elasticsearch/settings/nori-analyzer.json")
public class ContentDocument
{
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "content_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "content_analyzer")
    private String description;

    @Field(type = FieldType.Keyword)
    private String contentType;

    @Field(type = FieldType.Text, analyzer = "content_analyzer")
    private List<String> tags;

    @Field(type = FieldType.Float)
    private BigDecimal avgRating;

    public static ContentDocument from(Content content) {
        ContentDocument doc = new ContentDocument();

        doc.id = content.getId().toString();
        doc.title = content.getTitle();
        doc.description = content.getDescription();
        doc.contentType = content.getContentType().name();
        doc.tags = content.getTags();
        doc.avgRating = content.getAvgRating();
        return doc;
    }
}
