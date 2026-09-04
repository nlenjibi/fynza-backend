package ecommerce.modules.notification.mapper;

import ecommerce.modules.notification.dto.NotificationResponse;
import ecommerce.modules.notification.entity.Notification;
import org.mapstruct.*;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false))
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
