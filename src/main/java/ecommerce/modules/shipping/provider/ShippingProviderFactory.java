package ecommerce.modules.shipping.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ShippingProviderFactory {

    private final List<ShippingProvider> providers;

    public ShippingProvider getProvider(ShippingProviderType type) {
        return providers.stream()
                .filter(p -> p.providerType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No shipping provider for type: " + type));
    }

    public ShippingProvider getDefault() {
        return providers.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No shipping providers configured"));
    }
}
