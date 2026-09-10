package ecommerce.modules.store.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.store.dto.request.StoreSettingRequest;
import ecommerce.modules.store.dto.response.StoreSettingResponse;
import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.entity.StoreSetting;
import ecommerce.modules.store.mapper.StoreMapper;
import ecommerce.modules.store.policy.StoreOwnershipPolicy;
import ecommerce.modules.store.repository.StoreSettingRepository;
import ecommerce.modules.store.service.StoreSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreSettingServiceImpl implements StoreSettingService {

    private final StoreOwnershipPolicy   ownershipPolicy;
    private final StoreSettingRepository settingRepository;
    private final StoreMapper            mapper;
    private final AuditLogService        auditLogService;

    @Override
    public StoreSettingResponse getSettings(UUID actorUserId) {
        Store store = ownershipPolicy.resolveOwnStore(actorUserId);
        StoreSetting setting = settingRepository.findByStore_Id(store.getId())
                .orElseGet(() -> StoreSetting.builder().store(store).build());
        return mapper.toSettingResponse(setting);
    }

    @Override
    @Transactional
    public StoreSettingResponse updateSettings(UUID actorUserId, StoreSettingRequest request) {
        Store store = ownershipPolicy.resolveOwnStore(actorUserId);
        StoreSetting setting = settingRepository.findByStore_Id(store.getId())
                .orElseGet(() -> StoreSetting.builder().store(store).build());

        if (request.getCurrency() != null)             setting.setCurrency(request.getCurrency());
        if (request.getTimezone() != null)             setting.setTimezone(request.getTimezone());
        if (request.getLanguage() != null)             setting.setLanguage(request.getLanguage());
        if (request.getOrderNotifications() != null)   setting.setOrderNotifications(request.getOrderNotifications());
        if (request.getCustomerNotifications() != null) setting.setCustomerNotifications(request.getCustomerNotifications());
        settingRepository.save(setting);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.STORE_SETTINGS_UPDATED)
                .entityType("STORE")
                .entityPublicId(store.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toSettingResponse(setting);
    }
}
