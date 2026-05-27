package com.mopl.mopl.domain.dm.service;

import java.util.UUID;

public interface RoomPresenceManager {

  /**
   * 특정 유저가 현재 특정 대화방에 접속(웹소켓 구독) 중인지 확인
   * @param userId 확인할 유저의 ID
   * @param conversationId 접속 여부를 확인할 대화방 ID
   * @return 접속 중이면 true, 아니면 false
   */
  boolean isUserInRoom(UUID userId, UUID conversationId);
}
