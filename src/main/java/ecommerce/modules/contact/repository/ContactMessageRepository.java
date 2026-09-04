package ecommerce.modules.contact.repository;

import ecommerce.modules.contact.entity.ContactMessage;
import ecommerce.common.enums.ContactStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for ContactMessage entity.
 * Provides CRUD operations and custom queries for contact message management.
 */
@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {

    /**
     * Find contact messages by status with pagination.
     *
     * @param status   the contact status to filter by
     * @param pageable pagination information
     * @return page of contact messages
     */
    Page<ContactMessage> findByStatus(ContactStatus status, Pageable pageable);

    /**
     * Find contact messages by email containing search term.
     *
     * @param email    the email search term
     * @param pageable pagination information
     * @return page of contact messages
     */
    Page<ContactMessage> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    /**
     * Find contact messages by name containing search term.
     *
     * @param name     the name search term
     * @param pageable pagination information
     * @return page of contact messages
     */
    Page<ContactMessage> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Search contact messages by email or name.
     *
     * @param searchTerm the search term
     * @param pageable   pagination information
     * @return page of contact messages
     */
    @Query("SELECT c FROM ContactMessage c WHERE " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.subject) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<ContactMessage> searchMessages(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count messages by status.
     *
     * @param status the contact status
     * @return count of messages
     */
    long countByStatus(ContactStatus status);

    /**
     * Find contact messages assigned to a specific user with pagination.
     *
     * @param assignedTo the UUID of the assigned user
     * @param pageable  pagination information
     * @return page of contact messages
     */
    Page<ContactMessage> findByAssignedTo(UUID assignedTo, Pageable pageable);

    /**
     * Find contact messages assigned to a specific user with status filter.
     *
     * @param assignedTo the UUID of the assigned user
     * @param status     the contact status to filter by
     * @param pageable   pagination information
     * @return page of contact messages
     */
    Page<ContactMessage> findByAssignedToAndStatus(UUID assignedTo, ContactStatus status, Pageable pageable);

    /**
     * Count messages assigned to a specific user.
     *
     * @param assignedTo the UUID of the assigned user
     * @return count of messages
     */
    long countByAssignedTo(UUID assignedTo);

    /**
     * Count messages assigned to a specific user by status.
     *
     * @param assignedTo the UUID of the assigned user
     * @param status     the contact status
     * @return count of messages
     */
    long countByAssignedToAndStatus(UUID assignedTo, ContactStatus status);

    /**
     * Find contact messages by category with pagination.
     *
     * @param category the contact category
     * @param pageable pagination information
     * @return page of contact messages
     */
    Page<ContactMessage> findByCategory(ecommerce.common.enums.ContactCategory category, Pageable pageable);

    /**
     * Find contact messages by priority with pagination.
     *
     * @param priority the contact priority
     * @param pageable pagination information
     * @return page of contact messages
     */
    Page<ContactMessage> findByPriority(ecommerce.common.enums.ContactPriority priority, Pageable pageable);

    /**
     * Find unassigned contact messages (assignedTo is null) with pagination.
     *
     * @param pageable pagination information
     * @return page of unassigned contact messages
     */
    Page<ContactMessage> findByAssignedToIsNull(Pageable pageable);

    /**
     * Search contact messages by multiple fields including assigned user.
     *
     * @param searchTerm the search term
     * @param assignedTo optional assigned user filter
     * @param status     optional status filter
     * @param pageable   pagination information
     * @return page of contact messages
     */
    @Query("SELECT c FROM ContactMessage c WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.subject) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:assignedTo IS NULL OR c.assignedTo = :assignedTo) AND " +
           "(:status IS NULL OR c.status = :status)")
    Page<ContactMessage> searchMessagesWithFilters(
            @Param("searchTerm") String searchTerm,
            @Param("assignedTo") UUID assignedTo,
            @Param("status") ContactStatus status,
            Pageable pageable);
}
