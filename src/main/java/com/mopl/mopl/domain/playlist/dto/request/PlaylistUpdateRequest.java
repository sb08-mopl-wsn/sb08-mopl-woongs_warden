package com.mopl.mopl.domain.playlist.dto.request;

import jakarta.validation.constraints.Size;

public record PlaylistUpdateRequest(
    @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
    String title,

    String description
) {

}
