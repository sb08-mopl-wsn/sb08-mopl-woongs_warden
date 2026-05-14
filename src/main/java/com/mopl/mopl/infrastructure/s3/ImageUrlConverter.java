package com.mopl.mopl.infrastructure.s3;

import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUrlConverter
{
    @Value("${cloud.aws.s3.cdn-url}")
    private String cdnUrl;

    @Named("toThumbnailUrl")
    public String convert(String key) {
        if (key == null) return null;
        if (key.startsWith("http")) return key;
        return cdnUrl + "/" + key;
    }
}
