package ecommerce.modules.user.service.impl;

import ecommerce.common.enums.*;
import ecommerce.common.exception.DuplicateResourceException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.auth.service.SecurityService;
import ecommerce.modules.user.dto.*;
import ecommerce.modules.user.entity.Address;
import ecommerce.modules.user.entity.SellerProfile;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.AddressRepository;
import ecommerce.modules.user.repository.SellerProfileRepository;
import ecommerce.modules.user.repository.UserRepository;
import ecommerce.modules.user.service.UserService;
import ecommerce.common.util.TokenValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenValidationService tokenValidationService;
    private final SecurityService securityService;
    private static final String USER_NOT_FOUND = "User not found with id: ";

    // ── Conversion helpers ───────────────────────────────────────────────────

    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getPublicId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .profileImageUrl(user.getProfileImageUrl())
                .status(user.getStatus())
                .emailVerified(user.getIsEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt() != null
                        ? LocalDateTime.ofInstant(user.getCreatedAt(), ZoneId.systemDefault()) : null)
                .updatedAt(user.getUpdatedAt() != null
                        ? LocalDateTime.ofInstant(user.getUpdatedAt(), ZoneId.systemDefault()) : null)
                .build();
    }

    private AddressDto toAddressDto(Address address) {
        return AddressDto.builder()
                .id(address.getPublicId())
                .label(address.getLabel())
                .addressType(address.getAddressType())
                .streetAddress(address.getStreetAddress())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt() != null
                        ? LocalDateTime.ofInstant(address.getCreatedAt(), ZoneId.systemDefault()) : null)
                .updatedAt(address.getUpdatedAt() != null
                        ? LocalDateTime.ofInstant(address.getUpdatedAt(), ZoneId.systemDefault()) : null)
                .build();
    }

    private Address toAddress(AddressRequest request) {
        return Address.builder()
                .label(request.getLabel())
                .streetAddress(request.getStreetAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();
    }

    private User findUserByPublicId(UUID publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + publicId));
    }

    // ── User CRUD ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CachePut(value = "users", key = "#result.id")
    @CacheEvict(value = {
            "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public UserDto createUser(UserCreateRequest request) {
        if (request.getRole() != null && !request.getRole().isBlank()
                && !request.getRole().equalsIgnoreCase("USER") && !securityService.isAdmin()) {
            throw new AccessDeniedException("Only admins can assign roles");
        }
        if (userRepository.existsByUsernameAndIsActiveTrue(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmailAndIsActiveTrue(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        Role role = Role.CUSTOMER;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(role)
                .build();

        userRepository.save(user);
        log.info("User created with publicId: {}", user.getPublicId());
        return toUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public Optional<UserDto> getUserById(UUID id) {
        return Optional.of(toUserDto(findUserByPublicId(id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users-page", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserDto);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#userId")
    @CacheEvict(value = {
            "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public UserDto updateUser(UUID userId, UserUpdateRequest request) {
        securityService.checkSelfOrAdmin(userId);
        User user = findUserByPublicId(userId);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());

        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided for user update: {}", request.getRole());
            }
        }
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);
        log.info("User updated with publicId: {}", userId);
        return toUserDto(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "users", "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public void deleteUser(UUID id) {
        securityService.checkSelfOrAdmin(id);
        User user = findUserByPublicId(id);
        userRepository.delete(user);
        tokenValidationService.evictPrincipal(id);
        log.info("User deleted with publicId: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "admin-dashboard"}, allEntries = true)
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        securityService.checkSelfOrAdmin(userId);
        User user = findUserByPublicId(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("Password does not match");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChange(LocalDateTime.now());
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);
        log.info("Password changed for user with publicId: {}", userId);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#userId")
    @CacheEvict(value = {
            "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public UserDto updateUserRole(UUID userId, UpdateUserRoleRequest request) {
        User user = findUserByPublicId(userId);
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided for user update: {}", request.getRole());
            }
        }
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);
        log.info("User role updated for publicId: {}", userId);
        return toUserDto(user);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#userId")
    @CacheEvict(value = {
            "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public UserDto updateUserStatus(UUID userId, UserStatusRequest request) {
        securityService.checkSelfOrAdmin(userId);
        if (request.getIsActive() == null) {
            throw new IllegalArgumentException("isActive is required");
        }
        User user = findUserByPublicId(userId);
        user.setIsActive(request.getIsActive());
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);
        log.info("User status updated for publicId: {} to isActive={}", userId, request.getIsActive());
        return toUserDto(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "admin-dashboard"}, allEntries = true)
    public Boolean lockUserAccount(UUID userId) {
        User user = findUserByPublicId(userId);
        if (Boolean.TRUE.equals(user.getIsLocked())) return true;
        user.setIsLocked(true);
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);
        log.info("User account locked: {}", userId);
        return true;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "admin-dashboard"}, allEntries = true)
    public Boolean unlockUserAccount(UUID userId) {
        User user = findUserByPublicId(userId);
        if (!Boolean.TRUE.equals(user.getIsLocked())) return true;
        user.setIsLocked(false);
        userRepository.save(user);
        log.info("User account unlocked: {}", userId);
        return true;
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "users", "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public List<UserDto> bulkUpdateUsers(BulkUserUpdateRequest request) {
        List<UserDto> updatedUsers = new ArrayList<>();

        for (UUID userId : request.getUserIds()) {
            try {
                User user = userRepository.findByPublicId(userId).orElse(null);
                if (user == null) {
                    log.warn("User not found for bulk update: {}", userId);
                    continue;
                }
                if (request.getRole() != null && !request.getRole().isBlank()) {
                    try {
                        user.setRole(Role.valueOf(request.getRole().toUpperCase()));
                        userRepository.save(user);
                        tokenValidationService.evictPrincipal(userId);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid role in bulk update: {}", request.getRole());
                    }
                }
                if (request.getIsActive() != null) {
                    user.setIsActive(request.getIsActive());
                    userRepository.save(user);
                    tokenValidationService.evictPrincipal(userId);
                }
                updatedUsers.add(toUserDto(user));
            } catch (Exception e) {
                log.error("Error updating user {} in bulk update: {}", userId, e.getMessage());
            }
        }

        log.info("Bulk update completed: {} users updated out of {}",
                updatedUsers.size(), request.getUserIds().size());
        return updatedUsers;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users-predicate", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize).getBytes())")
    public Page<UserDto> findUsersWithPredicate(Specification<User> spec, Pageable pageable) {
        return userRepository.findAll(spec, pageable).map(this::toUserDto);
    }

    // ── Customer Profile ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "user-profile", key = "#userId")
    public UserDto getCustomerProfile(UUID userId) {
        return toUserDto(findUserByPublicId(userId));
    }

    @Override
    @Transactional
    @CachePut(value = "user-profile", key = "#userId")
    @CacheEvict(value = {"users"}, allEntries = true)
    public UserDto updateCustomerProfile(UUID userId, UserDto request) {
        User user = findUserByPublicId(userId);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());

        userRepository.save(user);
        log.info("Customer profile updated for user: {}", userId);
        return toUserDto(user);
    }

    // ── Address Operations ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> getCustomerAddresses(UUID userId) {
        if (userRepository.findByPublicId(userId).isEmpty()) {
            throw new ResourceNotFoundException(USER_NOT_FOUND + userId);
        }
        return addressRepository.findByUser_Id(userId).stream()
                .map(this::toAddressDto)
                .toList();
    }

    @Override
    @Transactional
    public AddressDto addCustomerAddress(UUID userId, AddressRequest request) {
        User user = findUserByPublicId(userId);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByUserPublicId(userId);
        }

        Address address = toAddress(request);
        address.setUser(user);

        Address savedAddress = addressRepository.save(address);
        log.info("Address added for user: {}", userId);
        return toAddressDto(savedAddress);
    }

    @Override
    @Transactional
    public AddressDto updateCustomerAddress(UUID userId, UUID addressId, AddressRequest request) {
        Address address = addressRepository.findByPublicId(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        if (!address.getUser().getPublicId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found with id: " + addressId);
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByUserPublicId(userId);
        }

        if (request.getLabel() != null) address.setLabel(request.getLabel());
        if (request.getStreetAddress() != null) address.setStreetAddress(request.getStreetAddress());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getState() != null) address.setState(request.getState());
        if (request.getPostalCode() != null) address.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null) address.setCountry(request.getCountry());
        if (request.getIsDefault() != null) address.setIsDefault(request.getIsDefault());

        Address updatedAddress = addressRepository.save(address);
        log.info("Address updated for user: {}", userId);
        return toAddressDto(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteCustomerAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByPublicId(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        if (!address.getUser().getPublicId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found with id: " + addressId);
        }

        addressRepository.delete(address);
        log.info("Address deleted for user: {}", userId);
    }

    // ── Statistics ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerStats() {
        long total = userRepository.countCustomers();
        long active = userRepository.countActiveCustomers();
        long blocked = userRepository.countCustomersByStatus(UserStatus.BLOCKED);
        long inactive = userRepository.countCustomersByStatus(UserStatus.INACTIVE);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCustomers", total);
        stats.put("activeCustomers", active);
        stats.put("blockedCustomers", blocked);
        stats.put("inactiveCustomers", inactive);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> searchCustomers(String query, String status, Pageable pageable) {
        UserStatus userStatus = null;
        if (status != null && !status.isBlank()) {
            userStatus = UserStatus.valueOf(status.toUpperCase());
        }
        return userRepository.searchCustomers(query, userStatus, pageable).map(this::toUserDto);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCustomersToCSV(String status) {
        log.info("Exporting customers to CSV - status: {}", status);

        UserStatus userStatus = null;
        if (status != null && !status.isBlank()) {
            userStatus = UserStatus.valueOf(status.toUpperCase());
        }

        List<User> users = userStatus != null
                ? userRepository.findByStatus(userStatus, Pageable.unpaged()).getContent()
                : userRepository.findAll();

        StringBuilder csv = new StringBuilder("Name,Email,Phone,Status,Joined Date,Last Login\n");
        for (User user : users) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(user.getFullName()),
                    escapeCsv(user.getEmail()),
                    escapeCsv(user.getPhone() != null ? user.getPhone() : ""),
                    user.getStatus(),
                    user.getCreatedAt(),
                    user.getLastLoginAt() != null ? user.getLastLoginAt() : ""));
        }
        return csv.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> searchSellers(String query, String status, Pageable pageable) {
        return sellerProfileRepository.searchSellers(query, null, pageable)
                .map(seller -> toUserDto(seller.getUser()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSellerStats() {
        long total = sellerProfileRepository.countAllSellers();
        long active = sellerProfileRepository.countBySellerStatus(SellerStatus.ACTIVE);
        long pending = sellerProfileRepository.countBySellerStatus(SellerStatus.PENDING);
        long suspended = sellerProfileRepository.countBySellerStatus(SellerStatus.SUSPENDED);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSellers", total);
        stats.put("activeSellers", active);
        stats.put("pendingSellers", pending);
        stats.put("suspendedSellers", suspended);
        return stats;
    }

    @Override
    @Transactional
    public UserDto approveSeller(UUID sellerId) {
        SellerProfile profile = sellerProfileRepository.findByPublicId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        profile.setSellerStatus(SellerStatus.ACTIVE);
        profile.setVerificationStatus(VerificationStatus.VERIFIED);
        sellerProfileRepository.save(profile);
        log.info("Seller {} approved", sellerId);
        return toUserDto(profile.getUser());
    }

    @Override
    @Transactional
    public UserDto suspendSeller(UUID sellerId) {
        SellerProfile profile = sellerProfileRepository.findByPublicId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        profile.setSellerStatus(SellerStatus.SUSPENDED);
        sellerProfileRepository.save(profile);
        log.info("Seller {} suspended", sellerId);
        return toUserDto(profile.getUser());
    }

    @Override
    @Transactional
    public UserDto reactivateSeller(UUID sellerId) {
        SellerProfile profile = sellerProfileRepository.findByPublicId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        profile.setSellerStatus(SellerStatus.ACTIVE);
        sellerProfileRepository.save(profile);
        log.info("Seller {} reactivated", sellerId);
        return toUserDto(profile.getUser());
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
