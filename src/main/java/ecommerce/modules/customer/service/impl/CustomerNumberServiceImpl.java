package ecommerce.modules.customer.service.impl;

import ecommerce.modules.customer.service.CustomerNumberService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerNumberServiceImpl implements CustomerNumberService {

    private final EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate() {
        Long next = (Long) entityManager
                .createNativeQuery("SELECT nextval('customer_number_seq')")
                .getSingleResult();
        return String.format("CUS-%06d", next);
    }
}
