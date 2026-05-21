package com.mopl.mopl.domain.auth.dto;

import com.mopl.mopl.domain.user.entity.Social;

public record OAuth2UserInfo(
        Social socialType,
        String socialId,
        String email,
        String name
) {
}