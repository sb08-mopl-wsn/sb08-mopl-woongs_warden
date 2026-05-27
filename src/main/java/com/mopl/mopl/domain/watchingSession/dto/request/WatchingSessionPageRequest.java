package com.mopl.mopl.domain.watchingSession.dto.request;

import com.mopl.mopl.domain.watchingSession.entity.SortDirection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record WatchingSessionPageRequest(

        String watcherNameLike,
        String cursor,
        UUID idAfter,

        @NotNull(message = "페이지를 가져올 개수는 필수입니다.")
        @Min(value = 1, message = "페이지를 가져올 개수는 1 이상이어야 합니다.")
        Integer limit,

        @NotNull(message = "정렬 방향은 필수값입니다.")
        SortDirection sortDirection,

        @NotNull(message = "정렬 기준은 필수 값입니다.")
        // 정렬 기준은 "createdAt"만 지원하기 때문에 다른 값이 들어올 경우 에러 발생
        @Pattern(regexp = "createdAt", message = "정렬 기준은 `createdAt' 만 지원합니다.")
        String sortBy
) {
        public WatchingSessionPageRequest {
                if (sortBy == null || sortBy.isBlank()) {
                        sortBy = "createdAt";
                }
        }
}
