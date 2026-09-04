package com.aoms.aomsbackend.auth.entity;

import lombok.Getter;

@Getter
public enum UserRoleType {
    INTERN(1),
    EMPLOYEE(2),
    FACILITIES_ADMIN(3),
    MANAGER(4),
    HR(5),
    SUPER_ADMIN(6);

    private final int rank;

    UserRoleType(int rank) {
        this.rank = rank;
    }

}
