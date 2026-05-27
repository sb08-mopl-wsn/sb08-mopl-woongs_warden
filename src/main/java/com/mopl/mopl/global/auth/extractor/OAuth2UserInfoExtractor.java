package com.mopl.mopl.global.auth.extractor;

import com.mopl.mopl.domain.auth.dto.OAuth2UserInfo;

import java.util.Map;

public interface OAuth2UserInfoExtractor {

    boolean supports(String registrationId);

    OAuth2UserInfo extract(Map<String, Object> attributes);
}