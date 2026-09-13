package ecommerce.modules.media.provider;

import ecommerce.modules.media.config.MediaProperties;
import ecommerce.modules.media.enums.ProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StorageProviderRouter {

    private final MediaProperties props;
    private final List<MediaStorageProvider> providers;

    private Map<ProviderType, MediaStorageProvider> providerMap;

    public MediaStorageProvider resolve() {
        return resolve(props.getProvider().getPrimary());
    }

    public MediaStorageProvider resolve(ProviderType type) {
        return providerMap().get(type);
    }

    private Map<ProviderType, MediaStorageProvider> providerMap() {
        if (providerMap == null) {
            providerMap = providers.stream()
                    .collect(Collectors.toMap(MediaStorageProvider::getProviderType, Function.identity()));
        }
        return providerMap;
    }
}
