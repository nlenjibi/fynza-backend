package ecommerce.modules.subscriber.service;

import ecommerce.modules.subscriber.dto.SubscriberRequest;
import ecommerce.modules.subscriber.dto.SubscriberResponse;
import ecommerce.modules.subscriber.entity.Subscriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SubscriberService {

    SubscriberResponse subscribe(SubscriberRequest request, String ipAddress);

    SubscriberResponse unsubscribe(UUID id);

    SubscriberResponse subscribeByEmail(String email, String ipAddress);

    Page<SubscriberResponse> getAllSubscribers(Subscriber.SubscriberStatus status, String search, Pageable pageable);

    SubscriberResponse getSubscriberById(UUID id);

    void deleteSubscriber(UUID id);

    String exportToCSV(Subscriber.SubscriberStatus status);

    long countTotalSubscribers();

    long countActiveSubscribers();

    long countUnsubscribedSubscribers();
}
