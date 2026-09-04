package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.BadgeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for {@link BadgeEvent} records.
 *
 * <p>Badge events are ingested by the data-engineering pipeline and treated as
 * read-only by this service.
 */
@Repository
public interface BadgeEventRepository extends JpaRepository<BadgeEvent, UUID> {

    /**
     * Returns the distinct set of user IDs that have at least one {@code BADGE_IN} event
     * at {@code buildingId} within the given time window.
     *
     * <p>This query is intentionally bulk: the caller passes the full set of user IDs from
     * a day's confirmed bookings, and the database filters in a single round-trip. This
     * avoids the N+1 query pattern that would result from checking each booking individually.
     *
     * @param buildingId the building where the badge event must have occurred
     * @param userIds    the candidate user IDs to check; must not be empty
     * @param from       start of the time window (inclusive), typically midnight UTC
     * @param to         end of the time window (exclusive), typically midnight UTC next day
     * @return set of user IDs who badged in; empty set if nobody did
     */
    @Query(value = """
            SELECT DISTINCT be.user_id
            FROM badge_events be
            WHERE be.building_id = :buildingId
              AND be.event_type = 'BADGE_IN'
              AND be.user_id IN :userIds
              AND be.occurred_at >= :from
              AND be.occurred_at < :to
            """, nativeQuery = true)
    Set<UUID> findUserIdsWithBadgeIn(
            @Param("buildingId") UUID buildingId,
            @Param("userIds") Collection<UUID> userIds,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
