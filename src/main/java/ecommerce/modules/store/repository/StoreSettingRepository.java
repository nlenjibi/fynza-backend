package ecommerce.modules.store.repository;

import ecommerce.modules.store.entity.StoreSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreSettingRepository extends JpaRepository<StoreSetting, Long> {

    Optional<StoreSetting> findByStore_Id(Long storeId);

    Optional<StoreSetting> findByPublicId(UUID publicId);
}
