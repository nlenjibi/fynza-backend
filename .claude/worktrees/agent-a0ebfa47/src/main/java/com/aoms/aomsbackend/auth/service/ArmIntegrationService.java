package com.aoms.aomsbackend.auth.service;

import com.aoms.aomsbackend.auth.dto.ArmProfile;

public interface ArmIntegrationService {

    ArmProfile fetchProfile(String armsUserId, String authToken);
}
