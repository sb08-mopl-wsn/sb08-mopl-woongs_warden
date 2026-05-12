package com.mopl.mopl.domain.watchingSession.repository;

import com.mopl.mopl.domain.watchingSession.dto.response.WatchingSessionSearchCondition;
import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WatchingSessionRepositoryCustom {

    List<WatchingSession> findAllByCursor(WatchingSessionSearchCondition condition, Pageable pageable);
}
