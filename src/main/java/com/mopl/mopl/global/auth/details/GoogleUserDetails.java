package com.mopl.mopl.global.auth.details;

import com.mopl.mopl.domain.user.dto.UserDto;

import java.util.Map;

public class GoogleUserDetails extends MoplUserDetails {

    public GoogleUserDetails(UserDto userDto, Map<String, Object> attributes) {
        super(userDto, null, attributes);
    }

    @Override
    public String getName() {
        return getUserDto().id().toString();
    }
}