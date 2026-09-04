package ecommerce.modules.coupon.service;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.coupon.dto.CouponRequest;
import ecommerce.modules.coupon.dto.CouponResponse;
import ecommerce.modules.coupon.entity.Coupon;
import ecommerce.modules.coupon.mapper.CouponMapper;
import ecommerce.modules.coupon.repository.CouponRepository;
import ecommerce.modules.coupon.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponServiceImpl Tests")
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponMapper couponMapper;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon testCoupon;
    private CouponResponse testCouponResponse;
    private CouponRequest testCouponRequest;
    private UUID couponId;

    @BeforeEach
    void setUp() {
        couponId = UUID.randomUUID();
        
        testCoupon = Coupon.builder()
                .id(couponId)
                .code("SAVE20")
                .description("Get 20% off your order")
                .discountType(ecommerce.common.enums.DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .minOrderAmount(BigDecimal.valueOf(50))
                .maxUses(100)
                .usageCount(0)
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(30))
                .status(ecommerce.common.enums.CouponStatus.ACTIVE)
                .isActive(true)
                .build();

        testCouponResponse = CouponResponse.builder()
                .id(couponId)
                .code("SAVE20")
                .description("Get 20% off your order")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(20))
                .minOrderAmount(BigDecimal.valueOf(50))
                .maxUses(100)
                .usageCount(0)
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(30))
                .status("ACTIVE")
                .build();

        testCouponRequest = CouponRequest.builder()
                .code("SAVE20")
                .description("Get 20% off your order")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(20))
                .minOrderAmount(BigDecimal.valueOf(50))
                .maxUses(100)
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("Should return all coupons")
        void findAll_ReturnsAllCoupons() {
            List<Coupon> coupons = Arrays.asList(testCoupon);
            when(couponRepository.findAll()).thenReturn(coupons);
            when(couponMapper.toResponse(any(Coupon.class))).thenReturn(testCouponResponse);

            List<CouponResponse> result = couponService.findAll();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("SAVE20", result.get(0).getCode());
            verify(couponRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no coupons exist")
        void findAll_WhenNoCoupons_ReturnsEmptyList() {
            when(couponRepository.findAll()).thenReturn(List.of());

            List<CouponResponse> result = couponService.findAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("Should return coupon when found")
        void findById_WhenCouponExists_ReturnsCoupon() {
            when(couponRepository.findById(couponId)).thenReturn(Optional.of(testCoupon));
            when(couponMapper.toResponse(any(Coupon.class))).thenReturn(testCouponResponse);

            CouponResponse result = couponService.findById(couponId);

            assertNotNull(result);
            assertEquals("SAVE20", result.getCode());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when coupon not found")
        void findById_WhenCouponNotFound_ThrowsException() {
            when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> couponService.findById(couponId));
        }
    }

    @Nested
    @DisplayName("findByCode")
    class FindByCodeTests {

        @Test
        @DisplayName("Should return coupon by code")
        void findByCode_WhenCouponExists_ReturnsCoupon() {
            when(couponRepository.findByCode("SAVE20")).thenReturn(Optional.of(testCoupon));
            when(couponMapper.toResponse(any(Coupon.class))).thenReturn(testCouponResponse);

            CouponResponse result = couponService.findByCode("SAVE20");

            assertNotNull(result);
            assertEquals("SAVE20", result.getCode());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when coupon code not found")
        void findByCode_WhenCouponNotFound_ThrowsException() {
            when(couponRepository.findByCode("INVALID")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> couponService.findByCode("INVALID"));
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("Should create coupon successfully")
        void create_WhenValidRequest_CreatesCoupon() {
            when(couponRepository.findByCode("SAVE20")).thenReturn(Optional.empty());
            when(couponMapper.toEntity(any(CouponRequest.class))).thenReturn(testCoupon);
            when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);
            when(couponMapper.toResponse(any(Coupon.class))).thenReturn(testCouponResponse);

            CouponResponse result = couponService.create(testCouponRequest);

            assertNotNull(result);
            assertEquals("SAVE20", result.getCode());
            verify(couponRepository, times(1)).save(any(Coupon.class));
        }

        @Test
        @DisplayName("Should throw exception when coupon code already exists")
        void create_WhenCodeExists_ThrowsException() {
            when(couponRepository.findByCode("SAVE20")).thenReturn(Optional.of(testCoupon));

            assertThrows(IllegalArgumentException.class,
                    () -> couponService.create(testCouponRequest));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("Should update coupon successfully")
        void update_WhenValidRequest_UpdatesCoupon() {
            when(couponRepository.findById(couponId)).thenReturn(Optional.of(testCoupon));
            when(couponRepository.findByCode("NEWCODE")).thenReturn(Optional.empty());
            when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);
            when(couponMapper.toResponse(any(Coupon.class))).thenReturn(testCouponResponse);

            CouponRequest updateRequest = CouponRequest.builder()
                    .code("NEWCODE")
                    .description("Updated Coupon")
                    .build();

            CouponResponse result = couponService.update(couponId, updateRequest);

            assertNotNull(result);
            verify(couponRepository, times(1)).save(any(Coupon.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent coupon")
        void update_WhenCouponNotFound_ThrowsException() {
            when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> couponService.update(couponId, testCouponRequest));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("Should delete coupon successfully")
        void delete_WhenCouponExists_DeletesCoupon() {
            when(couponRepository.findById(couponId)).thenReturn(Optional.of(testCoupon));
            doNothing().when(couponRepository).delete(testCoupon);

            couponService.delete(couponId);

            verify(couponRepository, times(1)).delete(testCoupon);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent coupon")
        void delete_WhenCouponNotFound_ThrowsException() {
            when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> couponService.delete(couponId));
        }
    }

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("Should validate coupon successfully")
        void validate_WhenValidCoupon_ReturnsCoupon() {
            when(couponRepository.findByCode("SAVE20")).thenReturn(Optional.of(testCoupon));

            Coupon result = couponService.validate("SAVE20", BigDecimal.valueOf(100));

            assertNotNull(result);
            assertEquals("SAVE20", result.getCode());
        }

        @Test
        @DisplayName("Should throw exception when coupon not found")
        void validate_WhenCouponNotFound_ThrowsException() {
            when(couponRepository.findByCode("INVALID")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> couponService.validate("INVALID", BigDecimal.valueOf(100)));
        }

        @Test
        @DisplayName("Should throw exception when coupon is not active")
        void validate_WhenCouponNotActive_ThrowsException() {
            testCoupon.setStatus(ecommerce.common.enums.CouponStatus.INACTIVE);
            when(couponRepository.findByCode("SAVE20")).thenReturn(Optional.of(testCoupon));

            assertThrows(IllegalStateException.class,
                    () -> couponService.validate("SAVE20", BigDecimal.valueOf(100)));
        }

        @Test
        @DisplayName("Should throw exception when coupon is expired")
        void validate_WhenCouponExpired_ThrowsException() {
            testCoupon.setValidUntil(LocalDateTime.now().minusDays(1));
            when(couponRepository.findByCode("SAVE20")).thenReturn(Optional.of(testCoupon));

            assertThrows(IllegalStateException.class,
                    () -> couponService.validate("SAVE20", BigDecimal.valueOf(100)));
        }

        @Test
        @DisplayName("Should throw exception when usage limit reached")
        void validate_WhenUsageLimitReached_ThrowsException() {
            testCoupon.setMaxUses(0);
            when(couponRepository.findByCode("SAVE20")).thenReturn(Optional.of(testCoupon));

            assertThrows(IllegalStateException.class,
                    () -> couponService.validate("SAVE20", BigDecimal.valueOf(100)));
        }

        @Test
        @DisplayName("Should throw exception when minimum order amount not met")
        void validate_WhenMinAmountNotMet_ThrowsException() {
            when(couponRepository.findByCode("SAVE20")).thenReturn(Optional.of(testCoupon));

            assertThrows(IllegalStateException.class,
                    () -> couponService.validate("SAVE20", BigDecimal.valueOf(30)));
        }
    }
}
