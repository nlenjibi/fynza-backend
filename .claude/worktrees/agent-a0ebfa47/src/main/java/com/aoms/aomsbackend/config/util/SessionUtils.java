package com.aoms.aomsbackend.config.util;

import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

public final class SessionUtils {
    private SessionUtils() {}

    public static UUID extractUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpSession session = attributes.getRequest().getSession(false);
        if (session == null) {
            return null;
        }
        return parseUuid(session.getAttribute(SessionAttribute.USER_ID.getKey()));
    }

    public static UUID parseUuid(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        try {
            return UUID.fromString(rawValue.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

