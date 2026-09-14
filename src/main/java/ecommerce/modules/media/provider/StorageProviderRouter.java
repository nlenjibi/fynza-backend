package ecommerce.modules.media.provider;

import ecommerce.modules.media.enums.ProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageProviderRouter {

    private final MediaStorageProvider provider;

    public MediaStorageProvider resolve() {
        return provider;
    }

    public MediaStorageProvider resolve(ProviderType type) {
        return provider;
    }
}
