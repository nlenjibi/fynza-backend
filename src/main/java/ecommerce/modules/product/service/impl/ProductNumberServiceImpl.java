package ecommerce.modules.product.service.impl;

import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.product.service.ProductNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductNumberServiceImpl implements ProductNumberService {

    private final ProductRepository productRepository;

    @Override
    public String nextProductNumber() {
        long count = productRepository.count() + 1;
        return String.format("PROD-%06d", count);
    }
}
