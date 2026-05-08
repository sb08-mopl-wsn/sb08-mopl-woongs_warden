package com.mopl.mopl.domain.follow.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FollowDto(
    @NotNull(message = "팔로우 ID는 필수입니다.")
    UUID id,
    @NotNull(message = "팔로우 대상 ID는 필수입니다.")
    UUID followeeId,
    @NotNull(message = "팔로우를 거는 주체 ID는 필수입니다.")
    UUID followerId
) {

}
