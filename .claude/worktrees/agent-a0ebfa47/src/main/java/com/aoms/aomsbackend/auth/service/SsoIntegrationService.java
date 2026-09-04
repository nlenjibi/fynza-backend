package com.aoms.aomsbackend.auth.service;

import com.aoms.aomsbackend.auth.dto.SSOUserProfile;

public interface SsoIntegrationService {

    SSOUserProfile fetchProfile(String appToken);
}
