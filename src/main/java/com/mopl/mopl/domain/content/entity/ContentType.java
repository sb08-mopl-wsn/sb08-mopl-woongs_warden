package com.mopl.mopl.domain.content.entity;

import com.mopl.mopl.domain.content.exception.ContentInvalidTypeException;

public enum ContentType {
    movie,
    tvSeries,
    sport;

    public static ContentType from(String value) {
        try {
            return ContentType.valueOf(value.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new ContentInvalidTypeException(value.toLowerCase());
        }
    }
}
