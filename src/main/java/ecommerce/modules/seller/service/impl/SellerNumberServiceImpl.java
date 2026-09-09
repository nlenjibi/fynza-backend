package ecommerce.modules.seller.service.impl;

import ecommerce.modules.seller.service.SellerNumberService;
import org.springframework.stereotype.Service;

@Service
public class SellerNumberServiceImpl implements SellerNumberService {

    private static final String PREFIX = "SEL-";

    @Override
    public String formatFromId(long id) {
        return PREFIX + String.format("%06d", id);
    }
}
