package com.mopl.mopl.domain.notification.mapper;

import com.mopl.mopl.domain.notification.dto.NotificationDto;
import com.mopl.mopl.domain.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mapping(source = "user.id", target = "receiverId")
  NotificationDto toDto(Notification notification);
}
