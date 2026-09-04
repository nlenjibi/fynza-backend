package com.aoms.aomsbackend.common.annotation;

import com.aoms.aomsbackend.auth.entity.UserRoleType;

import java.lang.annotation.*;

/**
 * The interface Requires role.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
    /**
     * Value user role type [ ].
     *
     * @return the user role type [ ]
     */
    UserRoleType value();
}
