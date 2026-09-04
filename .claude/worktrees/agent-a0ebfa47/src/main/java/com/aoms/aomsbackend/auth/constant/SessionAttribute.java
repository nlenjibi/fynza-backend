package com.aoms.aomsbackend.auth.constant;

import lombok.Getter;

/**
 * Session attribute keys used throughout the application.
 * Centralizes session attribute names to prevent typos and improve maintainability.
 */
@Getter
public enum SessionAttribute {

    /** User ID (internal OMS user UUID) */
    USER_ID("user_id"),

    /** User email address */
    EMAIL("email"),

    /** User roles list */
    ROLES("roles"),

    /** Department name from ARM */
    DEPARTMENT("department"),

    /** Position/job title from ARM */
    POSITION("position"),

    /** ARM user ID (external SSO user identifier) */
    ARMS_USER_ID("arms_user_id"),

    /** Access token for ARM GraphQL calls (if needed) */
    ACCESS_TOKEN("access_token"),

    FIRST_NAME("first_name"),
    LAST_NAME("last_name"),
    OTHER_NAME("other_name"),
    PROFILE_IMAGE("profile_image"),
    OFFICE("office"),
    ORGANIZATION("organization"),

    // V2 Auth (email/password) session attributes
    V2_USER_ID("V2_USER_ID"),
    V2_EMAIL("V2_EMAIL"),
    V2_ROLES("V2_ROLES"),
    V2_DEPARTMENT("V2_DEPARTMENT"),
    V2_POSITION("V2_POSITION");

    /**
     * -- GETTER --
     *  Get the session attribute key as a string.
     *
     * @return the session attribute key
     */
    private final String key;

    SessionAttribute(String key) {
        this.key = key;
    }

    /**
     * Get the session attribute key.
     *
     * @return the session attribute key
     */
    @Override
    public String toString() {
        return key;
    }
}