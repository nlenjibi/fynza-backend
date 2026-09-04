package ecommerce.modules.notification.dto;

import java.util.UUID;

/** Reference to the domain entity that triggered a notification (its public UUID and type label). */
public record EntityRef(String type, UUID id) {}
