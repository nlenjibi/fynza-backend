package ecommerce.modules.user.service.impl;

import ecommerce.common.enums.*;
import ecommerce.common.exception.DuplicateResourceException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.auth.service.SecurityService;
import ecommerce.modules.notification.entity.Notification;
import ecommerce.modules.notification.repository.NotificationRepository;
import ecommerce.modules.user.dto.*;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.entity.Address;
import ecommerce.modules.user.entity.SellerProfile;
import ecommerce.modules.user.mapper.UserMapper;
import ecommerce.modules.user.mapper.AddressMapper;
import ecommerce.modules.user.repository.UserRepository;
import ecommerce.modules.user.repository.AddressRepository;
import ecommerce.modules.user.repository.SellerProfileRepository;
import ecommerce.modules.user.service.UserService;
import ecommerce.common.util.TokenValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenValidationService tokenValidationService;
    private final NotificationRepository notificationRepository;
    private final SecurityService securityService;
    private static final String USER_NOT_FOUND = "User not found with id: ";


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

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        } else {
            user.setRole(Role.CUSTOMER);
        }

        userRepository.save(user);
        log.info("User created with id: {}", user.getId());
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public Optional<UserDto> getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
        return Optional.of(userMapper.toDto(user));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users-page", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        userMapper.updateEntity(user, request);
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided for user update: {}", request.getRole());
            }
        }
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);  // profile/role may have changed
        log.info("User updated with id: {}", userId);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "users",
            "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public void deleteUser(UUID id) {
        securityService.checkSelfOrAdmin(id);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND + id);
        }
        userRepository.deleteById(id);
        tokenValidationService.evictPrincipal(id);   // user gone – clear cached principal
        log.info("User deleted with id: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "admin-dashboard"}, allEntries = true)
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        securityService.checkSelfOrAdmin(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("Password does not match");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChange(LocalDateTime.now());
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);  // password changed – stale principal must not be served
        log.info("Password changed for user with id: {}", userId);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#userId")
    @CacheEvict(value = {
            "users-page", "users-search", "users-role", "users-active",
            "users-predicate", "admin-dashboard"
    }, allEntries = true)
    public UserDto updateUserRole(UUID userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        
        Role oldRole = user.getRole();
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided for user update: {}", request.getRole());
            }
        }
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);  // role changed – authorities in cached principal are stale
        
        // Send notification about role change
        sendRoleChangeNotification(user, oldRole, user.getRole());
        
        log.info("User role updated for user with id: {}", userId);
        return userMapper.toDto(user);
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        
        boolean oldStatus = user.getIsActive();
        user.setIsActive(request.getIsActive());
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);  // isActive changed – enabled() in cached principal is stale
        
        // Send notification about status change
        sendStatusChangeNotification(user, oldStatus, request.getIsActive());
        
        log.info("User status updated for user with id: {} to isActive={}", userId, request.getIsActive());
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "admin-dashboard"}, allEntries = true)
    public Boolean lockUserAccount(UUID userId) {
        log.info("Locking user account: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        
        if (user.getIsLocked() != null && user.getIsLocked()) {
            log.warn("User account already locked: {}", userId);
            return true;
        }
        
        user.setIsLocked(true);
        userRepository.save(user);
        tokenValidationService.evictPrincipal(userId);
        
        log.info("User account locked successfully: {}", userId);
        return true;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "admin-dashboard"}, allEntries = true)
    public Boolean unlockUserAccount(UUID userId) {
        log.info("Unlocking user account: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        
        if (user.getIsLocked() == null || !user.getIsLocked()) {
            log.warn("User account already unlocked: {}", userId);
            return true;
        }
        
        user.setIsLocked(false);
        userRepository.save(user);
        
        log.info("User account unlocked successfully: {}", userId);
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
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    log.warn("User not found for bulk update: {}", userId);
                    continue;
                }
                
                // Update role if provided
                if (request.getRole() != null && !request.getRole().isBlank()) {
                    try {
                        Role oldRole = user.getRole();
                        user.setRole(Role.valueOf(request.getRole().toUpperCase()));
                        userRepository.save(user);
                        
                        if (Boolean.TRUE.equals(request.getSendNotification())) {
                            sendRoleChangeNotification(user, oldRole, user.getRole());
                        }
                        tokenValidationService.evictPrincipal(userId);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid role in bulk update: {}", request.getRole());
                    }
                }
                
                // Update status if provided
                if (request.getIsActive() != null) {
                    boolean oldStatus = user.getIsActive();
                    user.setIsActive(request.getIsActive());
                    userRepository.save(user);
                    
                    if (Boolean.TRUE.equals(request.getSendNotification())) {
                        sendStatusChangeNotification(user, oldStatus, request.getIsActive());
                    }
                    tokenValidationService.evictPrincipal(userId);
                }
                
                updatedUsers.add(userMapper.toDto(user));
            } catch (Exception e) {
                log.error("Error updating user {} in bulk update: {}", userId, e.getMessage());
            }
        }
        
        log.info("Bulk update completed: {} users updated out of {}", 
                updatedUsers.size(), request.getUserIds().size());
        return updatedUsers;
    }

    /**
     * Send notification to user about role change
     */
    private void sendRoleChangeNotification(User user, Role oldRole, Role newRole) {
        try {
            String message = String.format(
                    "Your account role has been changed from %s to %s.",
                    oldRole != null ? oldRole.name() : "NONE",
                    newRole != null ? newRole.name() : "NONE");
            
            Notification notification = Notification.builder()
                    .user(user)
                    .type("ROLE_CHANGE")
                    .title("Role Updated")
                    .message(message)
                    .isRead(false)
                    .build();
            
            notificationRepository.save(notification);
            log.debug("Role change notification sent to user {}", user.getId());
        } catch (Exception e) {
            log.warn("Failed to send role change notification to user {}: {}", 
                    user.getId(), e.getMessage());
        }
    }

    /**
     * Send notification to user about status change
     */
    private void sendStatusChangeNotification(User user, boolean oldStatus, boolean newStatus) {
        try {
            String message;
            if (oldStatus && !newStatus) {
                message = "Your account has been deactivated. Please contact support for assistance.";
            } else if (!oldStatus && newStatus) {
                message = "Your account has been activated. You can now log in to your account.";
            } else {
                return; // No meaningful change
            }
            
            Notification notification = Notification.builder()
                    .user(user)
                    .type("STATUS_CHANGE")
                    .title(newStatus ? "Account Activated" : "Account Deactivated")
                    .message(message)
                    .isRead(false)
                    .build();
            
            notificationRepository.save(notification);
            log.debug("Status change notification sent to user {}", user.getId());
        } catch (Exception e) {
            log.warn("Failed to send status change notification to user {}: {}", 
                    user.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users-predicate", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(('#predicate=' + #predicate.toString() + '&page=' + #pageable.pageNumber + '&size=' + #pageable.pageSize + '&sort=' + #pageable.sort).getBytes())")
    public Page<UserDto> findUsersWithPredicate(com.querydsl.core.types.Predicate predicate, Pageable pageable) {
        return userRepository.findAll(predicate, pageable).map(userMapper::toDto);
    }

    // ==================== Customer Profile Operations ====================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "user-profile", key = "#userId")
    public UserDto getCustomerProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    @CachePut(value = "user-profile", key = "#userId")
    @CacheEvict(value = {"users"}, allEntries = true)
    public UserDto updateCustomerProfile(UUID userId, UserDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        User updatedUser = userRepository.save(user);
        log.info("Customer profile updated for user: {}", userId);
        return userMapper.toDto(updatedUser);
    }

    // ==================== Address Operations ====================

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> getCustomerAddresses(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND + userId);
        }
        List<Address> addresses = addressRepository.findByUserId(userId);
        return addresses.stream()
                .map(addressMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AddressDto addCustomerAddress(UUID userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByUserId(userId);
        }

        Address address = addressMapper.toEntity(request);
        address.setUser(user);

        Address savedAddress = addressRepository.save(address);
        log.info("Address added for user: {}", userId);
        return addressMapper.toDto(savedAddress);
    }

    @Override
    @Transactional
    public AddressDto updateCustomerAddress(UUID userId, UUID addressId, AddressRequest request) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found with id: " + addressId);
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByUserId(userId);
        }

        if (request.getLabel() != null) {
            address.setLabel(request.getLabel());
        }
        if (request.getStreetAddress() != null) {
            address.setStreetAddress(request.getStreetAddress());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getState() != null) {
            address.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            address.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        Address updatedAddress = addressRepository.save(address);
        log.info("Address updated for user: {}", userId);
        return addressMapper.toDto(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteCustomerAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found with id: " + addressId);
        }

        addressRepository.delete(address);
        log.info("Address deleted for user: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerStats() {
        log.debug("Fetching customer statistics");

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
        log.debug("Searching customers - query: {}, status: {}", query, status);

        UserStatus userStatus = null;
        if (status != null && !status.isBlank()) {
            userStatus = UserStatus.valueOf(status.toUpperCase());
        }

        Page<User> users = userRepository.searchCustomers(query, userStatus, pageable);
        return users.map(userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCustomersToCSV(String status) {
        log.info("Exporting customers to CSV - status: {}", status);

        UserStatus userStatus = null;
        if (status != null && !status.isBlank()) {
            userStatus = UserStatus.valueOf(status.toUpperCase());
        }

        List<User> users;
        if (userStatus != null) {
            users = userRepository.findByStatus(userStatus, Pageable.unpaged()).getContent();
        } else {
            users = userRepository.findAll();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Name,Email,Phone,Status,Joined Date,Last Login\n");

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

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> searchSellers(String query, String status, Pageable pageable) {
        log.debug("Searching sellers - query: {}, status: {}", query, status);

        UserStatus userStatus = null;
        if (status != null && !status.isBlank()) {
            userStatus = UserStatus.valueOf(status.toUpperCase());
        }

        Page<SellerProfile> sellers = sellerProfileRepository.searchSellers(query, null, pageable);
        return sellers.map(seller -> userMapper.toDto(seller.getUser()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSellerStats() {
        log.debug("Fetching seller statistics");

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
        log.info("Approving seller: {}", sellerId);

        SellerProfile profile = sellerProfileRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        profile.setSellerStatus(SellerStatus.ACTIVE);
        profile.setVerificationStatus(VerificationStatus.VERIFIED);
        sellerProfileRepository.save(profile);

        log.info("Seller {} approved successfully", sellerId);
        return userMapper.toDto(profile.getUser());
    }

    @Override
    @Transactional
    public UserDto suspendSeller(UUID sellerId) {
        log.info("Suspending seller: {}", sellerId);

        SellerProfile profile = sellerProfileRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        profile.setSellerStatus(SellerStatus.SUSPENDED);
        sellerProfileRepository.save(profile);

        log.info("Seller {} suspended successfully", sellerId);
        return userMapper.toDto(profile.getUser());
    }

    @Override
    @Transactional
    public UserDto reactivateSeller(UUID sellerId) {
        log.info("Reactivating seller: {}", sellerId);

        SellerProfile profile = sellerProfileRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        profile.setSellerStatus(SellerStatus.ACTIVE);
        sellerProfileRepository.save(profile);

        log.info("Seller {} reactivated successfully", sellerId);
        return userMapper.toDto(profile.getUser());
    }
}
