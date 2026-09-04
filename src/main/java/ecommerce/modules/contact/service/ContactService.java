package ecommerce.modules.contact.service;

import ecommerce.common.enums.ContactCategory;
import ecommerce.common.enums.ContactPriority;
import ecommerce.common.enums.ContactStatus;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.contact.dto.ContactMessageRequest;
import ecommerce.modules.contact.dto.ContactMessageResponse;
import ecommerce.modules.contact.dto.ContactResponseRequest;
import ecommerce.modules.contact.dto.ContactStats;
import ecommerce.modules.contact.entity.ContactMessage;
import ecommerce.modules.contact.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMessageRepository contactMessageRepository;

    @Transactional
    public ContactMessageResponse createMessage(ContactMessageRequest request) {
        log.info("Creating new contact message from: {}", request.getEmail());

        ContactMessage message = ContactMessage.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .subject(request.getSubject())
                .message(request.getMessage())
                .status(ContactStatus.NEW)
                .priority(ContactPriority.MEDIUM)
                .category(request.getCategory() != null ? request.getCategory() :  ContactCategory.GENERAL_INQUIRY)
                .build();

        ContactMessage savedMessage = contactMessageRepository.save(message);
        log.info("Contact message created with ID: {}", savedMessage.getId());

        return ContactMessageResponse.from(savedMessage);
    }

    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> getAllMessages(ContactStatus status, Pageable pageable) {
        log.debug("Fetching contact messages with status: {}", status);

        if (status != null) {
            return contactMessageRepository.findByStatus(status, pageable)
                    .map(ContactMessageResponse::from);
        }
        return contactMessageRepository.findAll(pageable)
                .map(ContactMessageResponse::from);
    }

    @Transactional(readOnly = true)
    public ContactMessageResponse getMessageById(UUID id) {
        log.debug("Fetching contact message with ID: {}", id);

        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found with ID: " + id));

        return ContactMessageResponse.from(message);
    }

    @Transactional
    public ContactMessageResponse respondToMessage(UUID id, ContactResponseRequest request) {
        log.info("Admin responding to contact message ID: {}", id);

        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found with ID: " + id));

        message.setAdminResponse(request.getAdminResponse());
        message.setRespondedAt(LocalDateTime.now());
        message.setRespondedBy(request.getAdminId());
        message.setStatus(ContactStatus.RESPONDED);

        ContactMessage updatedMessage = contactMessageRepository.save(message);
        log.info("Contact message {} responded by admin {}", id, request.getAdminId());

        return ContactMessageResponse.from(updatedMessage);
    }

    @Transactional
    public ContactMessageResponse updateMessageStatus(UUID id, ContactStatus status) {
        log.info("Updating contact message {} status to: {}", id, status);

        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found with ID: " + id));

        message.setStatus(status);
        ContactMessage updatedMessage = contactMessageRepository.save(message);

        return ContactMessageResponse.from(updatedMessage);
    }

    @Transactional
    public ContactMessageResponse assignMessage(UUID id, UUID assignedToId) {
        log.info("Assigning contact message {} to user: {}", id, assignedToId);

        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found with ID: " + id));

        message.setAssignedTo(assignedToId);
        
        // Update status to IN_PROGRESS if it's still NEW
        if (message.getStatus() == ContactStatus.NEW) {
            message.setStatus(ContactStatus.IN_PROGRESS);
        }

        ContactMessage updatedMessage = contactMessageRepository.save(message);
        log.info("Contact message {} assigned to user {} successfully", id, assignedToId);

        return ContactMessageResponse.from(updatedMessage);
    }

    @Transactional
    public ContactMessageResponse categorizeMessage(UUID id, ContactCategory category, ContactPriority priority) {
        log.info("Categorizing contact message {} - category: {}, priority: {}", id, category, priority);

        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found with ID: " + id));

        if (category != null) {
            message.setCategory(category);
        }
        if (priority != null) {
            message.setPriority(priority);
        }

        ContactMessage updatedMessage = contactMessageRepository.save(message);
        log.info("Contact message {} categorized successfully", id);

        return ContactMessageResponse.from(updatedMessage);
    }

    @Transactional
    public void deleteMessage(UUID id) {
        log.info("Deleting contact message with ID: {}", id);

        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found with ID: " + id));

        message.setIsActive(false);
        contactMessageRepository.save(message);
        log.info("Contact message {} soft deleted", id);
    }

    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> searchMessages(String searchTerm, Pageable pageable) {
        log.debug("Searching contact messages with term: {}", searchTerm);

        return contactMessageRepository.searchMessages(searchTerm, pageable)
                .map(ContactMessageResponse::from);
    }

    @Transactional(readOnly = true)
    public long countMessagesByStatus(ContactStatus status) {
        return contactMessageRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countTotalMessages() {
        return contactMessageRepository.count();
    }

    @Transactional
    public ContactMessageResponse submitMessage(ContactMessageRequest request) {
        return createMessage(request);
    }

    @Transactional(readOnly = true)
    public ContactStats getMessageStats() {
        log.debug("Fetching contact message statistics");
        
        return ContactStats.builder()
                .total(countTotalMessages())
                .newCount(countMessagesByStatus(ContactStatus.NEW))
                .inProgress(countMessagesByStatus(ContactStatus.IN_PROGRESS))
                .responded(countMessagesByStatus(ContactStatus.RESPONDED))
                .closed(countMessagesByStatus(ContactStatus.CLOSED))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> getMessagesByAssignedTo(UUID assignedTo, Pageable pageable) {
        log.debug("Fetching contact messages assigned to user: {}", assignedTo);
        
        return contactMessageRepository.findByAssignedTo(assignedTo, pageable)
                .map(ContactMessageResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> getMessagesByAssignedToAndStatus(UUID assignedTo, ContactStatus status, Pageable pageable) {
        log.debug("Fetching contact messages assigned to user: {} with status: {}", assignedTo, status);
        
        return contactMessageRepository.findByAssignedToAndStatus(assignedTo, status, pageable)
                .map(ContactMessageResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> getUnassignedMessages(Pageable pageable) {
        log.debug("Fetching unassigned contact messages");
        
        return contactMessageRepository.findByAssignedToIsNull(pageable)
                .map(ContactMessageResponse::from);
    }
}
