package com.mopl.mopl.domain.user.dto.request;

import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.SortBy;
import com.mopl.mopl.domain.user.entity.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CursorUserRequest(
        String emailLike,
        Role roleEqual,
        String cursor,
        UUID idAfter,
        @NotNull(message = "limit는 필수값입니다.")
        @Min(value = 1, message = "limit는 1 이상이어야 합니다.")
        @Max(value = 100, message = "limit은 100 이하의 값이어야 합니다.")
        Integer limit,
        @NotNull(message = "sortDirection은 필수값입니다.")
        SortDirection sortDirection,
        @NotNull(message = "sortBy는 필수값입니다.")
        SortBy sortBy
) {
    public CursorUserRequest {
        boolean cursorSet = cursor != null && !cursor.isBlank();
        boolean idAfterSet = idAfter != null;
        if (cursorSet != idAfterSet) {
            throw new IllegalArgumentException("cursor와 idAfter는 함께 전달되어야 합니다.");
        }
    }
}