package ecommerce.modules.user.repository;

import com.querydsl.core.types.Predicate;
import ecommerce.common.base.BaseRepository;
import ecommerce.common.enums.PaymentMethod;
import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import ecommerce.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends BaseRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIsActiveTrue(String email);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIsActiveTrue(String username);

    @Query("SELECT u FROM User u WHERE u.role = :role")
    Page<User> findByRole(@Param("role") Role role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.status = :status")
    Page<User> findByStatus(@Param("status") UserStatus status, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();

    @Modifying
    @Query("UPDATE User u SET u.isActive = false, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int deactivateUser(@Param("userId") UUID userId);

    Page<User> findAll(Predicate predicate, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = 'CUSTOMER' " +
           "AND (:query IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<User> searchCustomers(@Param("query") String query, @Param("status") UserStatus status, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'CUSTOMER'")
    long countCustomers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'CUSTOMER' AND u.status = :status")
    long countCustomersByStatus(@Param("status") UserStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'CUSTOMER' AND u.isActive = true")
    long countActiveCustomers();
}
