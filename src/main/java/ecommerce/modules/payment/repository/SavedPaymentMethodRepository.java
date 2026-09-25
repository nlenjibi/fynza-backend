package ecommerce.modules.payment.repository;

import ecommerce.modules.payment.entity.SavedPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, UUID> {

    List<SavedPaymentMethod> findByUserIdOrderByIsDefaultDesc(UUID userId);

    @Modifying
    @Query("UPDATE SavedPaymentMethod p SET p.isDefault = false WHERE p.userId = :userId")
    void clearDefaultPaymentMethods(@Param("userId") UUID userId);

    @Query("SELECT p FROM SavedPaymentMethod p WHERE p.expiryMonth <= :month AND p.expiryYear = :year AND p.methodType = 'CARD'")
    List<SavedPaymentMethod> findExpiringCards(@Param("month") int month, @Param("year") int year);
}
