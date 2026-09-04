package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "no_show_record_read_model")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoShowReadModel {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "no_show_record_id")
    private UUID noShowRecordId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "organisation_id")
    private UUID organisationId;

    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(name = "seat_reference")
    private String seatReference;

    @Column(name = "auto_released_at")
    private Instant autoReleasedAt;

    @Column(name = "created_at")
    private Instant createdAt;
}
