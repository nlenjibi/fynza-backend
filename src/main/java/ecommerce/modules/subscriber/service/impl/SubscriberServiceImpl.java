package ecommerce.modules.subscriber.service.impl;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.subscriber.dto.SubscriberRequest;
import ecommerce.modules.subscriber.dto.SubscriberResponse;
import ecommerce.modules.subscriber.entity.Subscriber;
import ecommerce.modules.subscriber.repository.SubscriberRepository;
import ecommerce.modules.subscriber.service.SubscriberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriberServiceImpl implements SubscriberService {

    private final SubscriberRepository subscriberRepository;

    @Override
    @Transactional
    public SubscriberResponse subscribe(SubscriberRequest request, String ipAddress) {
        log.info("Processing new subscription for email: {}", request.getEmail());

        String email = request.getEmail().toLowerCase().trim();

        if (subscriberRepository.existsByEmail(email)) {
            Subscriber existing = subscriberRepository.findByEmail(email)
                    .orElseThrow(() -> new BadRequestException("Subscriber already exists"));

            if (existing.getStatus() == Subscriber.SubscriberStatus.ACTIVE) {
                throw new BadRequestException("Email is already subscribed");
            }

            existing.setStatus(Subscriber.SubscriberStatus.ACTIVE);
            existing.setSubscribedAt(LocalDateTime.now());
            existing.setUnsubscribedAt(null);
            existing.setIpAddress(ipAddress);
            Subscriber saved = subscriberRepository.save(existing);
            log.info("Re-subscribed existing subscriber: {}", saved.getId());
            return SubscriberResponse.from(saved);
        }

        Subscriber subscriber = Subscriber.builder()
                .email(email)
                .status(Subscriber.SubscriberStatus.ACTIVE)
                .subscribedAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .build();

        Subscriber saved = subscriberRepository.save(subscriber);
        log.info("New subscriber created with ID: {}", saved.getId());

        return SubscriberResponse.from(saved);
    }

    @Override
    @Transactional
    public SubscriberResponse subscribeByEmail(String email, String ipAddress) {
        SubscriberRequest request = new SubscriberRequest();
        request.setEmail(email);
        return subscribe(request, ipAddress);
    }

    @Override
    @Transactional
    public SubscriberResponse unsubscribe(UUID id) {
        log.info("Unsubscribing subscriber with ID: {}", id);

        Subscriber subscriber = subscriberRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found"));

        subscriber.setStatus(Subscriber.SubscriberStatus.UNSUBSCRIBED);
        subscriber.setUnsubscribedAt(LocalDateTime.now());

        Subscriber saved = subscriberRepository.save(subscriber);
        log.info("Subscriber {} unsubscribed successfully", id);

        return SubscriberResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriberResponse> getAllSubscribers(Subscriber.SubscriberStatus status, String search, Pageable pageable) {
        log.debug("Fetching subscribers - status: {}, search: {}", status, search);

        Page<Subscriber> subscribers;

        if (search != null && !search.isBlank()) {
            if (status != null) {
                subscribers = subscriberRepository.searchByEmailAndStatus(search, status, pageable);
            } else {
                subscribers = subscriberRepository.searchByEmail(search, pageable);
            }
        } else if (status != null) {
            subscribers = subscriberRepository.findByStatus(status, pageable);
        } else {
            subscribers = subscriberRepository.findAll(pageable);
        }

        return subscribers.map(SubscriberResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriberResponse getSubscriberById(UUID id) {
        Subscriber subscriber = subscriberRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));
        return SubscriberResponse.from(subscriber);
    }

    @Override
    @Transactional
    public void deleteSubscriber(UUID id) {
        log.info("Deleting subscriber with ID: {}", id);

        Subscriber subscriber = subscriberRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));

        subscriber.setIsActive(false);
        subscriberRepository.save(subscriber);
        log.info("Subscriber {} deleted successfully", id);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportToCSV(Subscriber.SubscriberStatus status) {
        log.info("Exporting subscribers to CSV - status: {}", status);

        List<Subscriber> subscribers;
        if (status != null) {
            subscribers = subscriberRepository.findByStatus(status, Pageable.unpaged()).getContent();
        } else {
            subscribers = subscriberRepository.findAll();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Email,Status,Subscribed At,Unsubscribed At,Created At\n");

        for (Subscriber subscriber : subscribers) {
            csv.append(String.format("%s,%s,%s,%s,%s\n",
                    escapeCsv(subscriber.getEmail()),
                    subscriber.getStatus(),
                    subscriber.getSubscribedAt(),
                    subscriber.getUnsubscribedAt() != null ? subscriber.getUnsubscribedAt() : "",
                    subscriber.getCreatedAt()));
        }

        return csv.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalSubscribers() {
        return subscriberRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveSubscribers() {
        return subscriberRepository.countByStatus(Subscriber.SubscriberStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnsubscribedSubscribers() {
        return subscriberRepository.countByStatus(Subscriber.SubscriberStatus.UNSUBSCRIBED);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
