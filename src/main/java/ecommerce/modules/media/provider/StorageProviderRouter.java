package ecommerce.modules.media.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageProviderRouter {

    private final MediaStorageProvider provider;

    public MediaStorageProvider resolve() {
        return provider;
    }
}
