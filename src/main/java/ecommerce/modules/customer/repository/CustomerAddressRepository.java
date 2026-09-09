package ecommerce.modules.customer.repository;

import ecommerce.modules.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    List<CustomerAddress> findByCustomer_IdAndIsActiveTrue(Long customerId);

    Optional<CustomerAddress> findByPublicId(UUID publicId);

    Optional<CustomerAddress> findByPublicIdAndCustomer_Id(UUID publicId, Long customerId);

    int countByCustomer_IdAndIsActiveTrue(Long customerId);

    @Modifying
    @Query("UPDATE CustomerAddress a SET a.isDefault = false WHERE a.customer.id = :customerId")
    void clearDefaultByCustomerId(@Param("customerId") Long customerId);
}
