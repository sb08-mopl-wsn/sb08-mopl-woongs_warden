package com.mopl.mopl.global.auth.details;

import com.mopl.mopl.domain.user.dto.UserDto;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public class GoogleUserDetails extends MoplUserDetails implements OAuth2User {
    private final Map<String, Object> attributes;

    public GoogleUserDetails(UserDto userDto, Map<String, Object> attributes) {
        super(userDto, null);
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return getUserDto().id().toString();
    }
}