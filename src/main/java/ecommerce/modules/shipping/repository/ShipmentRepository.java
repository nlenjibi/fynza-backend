package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.Shipment;
import ecommerce.modules.shipping.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByPublicId(UUID publicId);
    Optional<Shipment> findByShipmentNumber(String shipmentNumber);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    boolean existsByShipmentNumber(String shipmentNumber);
    List<Shipment> findByFulfillment_PublicId(UUID fulfillmentPublicId);

    @Query("""
            SELECT s FROM Shipment s
            JOIN s.fulfillment f
            WHERE f.orderId = :orderId
            """)
    List<Shipment> findByOrderId(@Param("orderId") UUID orderId);

    @Query("""
            SELECT s FROM Shipment s
            JOIN s.fulfillment f
            WHERE f.sellerId = :sellerId AND s.status = :status
            """)
    List<Shipment> findBySellerIdAndStatus(@Param("sellerId") Long sellerId,
                                           @Param("status") ShipmentStatus status);
}
