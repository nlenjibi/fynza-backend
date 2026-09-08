package ecommerce.modules.user.service;

import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.auth.service.SecurityService;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.user.dto.AccountDeactivationRequest;
import ecommerce.modules.user.dto.AccountDeletionRequest;
import ecommerce.modules.user.dto.AdminSuspendRequest;
import ecommerce.modules.user.dto.AdminUserSearchParams;
import ecommerce.modules.user.dto.UpdateProfileRequest;
import ecommerce.modules.user.dto.UserProfileResponse;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.AddressRepository;
import ecommerce.modules.user.repository.CustomerProfileRepository;
import ecommerce.modules.user.repository.SellerProfileRepository;
import ecommerce.modules.user.repository.UserRepository;
import ecommerce.modules.user.service.impl.UserServiceImpl;
import ecommerce.modules.wishlist.repository.WishlistItemRepository;
import ecommerce.common.util.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no Spring context) for the user-management methods added to
 * {@link UserServiceImpl}.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code getMyProfile}</li>
 *   <li>{@code updateMyProfile}</li>
 *   <li>{@code deactivateAccount}</li>
 *   <li>{@code requestAccountDeletion}</li>
 *   <li>{@code adminSearchUsers}</li>
 *   <li>{@code adminGetUser}</li>
 *   <li>{@code adminSuspendUser}</li>
 *   <li>{@code adminActivateUser}</li>
 *   <li>{@code adminDisableUser}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl — user management methods")
class UserManagementServiceTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────

    @Mock private UserRepository              userRepository;
    @Mock private AddressRepository           addressRepository;
    @Mock private SellerProfileRepository     sellerProfileRepository;
    @Mock private CustomerProfileRepository   customerProfileRepository;
    @Mock private WishlistItemRepository      wishlistItemRepository;
    @Mock private OrderRepository             orderRepository;
    @Mock private PasswordEncoder             passwordEncoder;
    @Mock private TokenValidationService      tokenValidationService;
    @Mock private SecurityService             securityService;

    @InjectMocks
    private UserServiceImpl userService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private UUID   userId;
    private User   activeUser;

    @BeforeEach
    void setUpFixtures() {
        userId = UUID.randomUUID();

        activeUser = User.builder()
                .id(userId)
                .email("alice@example.com")
                .username("alice")
                .firstName("Alice")
                .lastName("Smith")
                .displayName("Ali")
                .phone("+233201234567")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        // Manually set publicId-equivalent — getPublicId() returns id when publicId is null
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Stubs the repository lookup so findUserByPublicId() succeeds. */
    private void givenUserExists(User user) {
        when(userRepository.findByPublicId(user.getId()))
                .thenReturn(Optional.of(user));
    }

    /** Stubs the repository lookup to return empty (user not found). */
    private void givenUserNotFound(UUID id) {
        when(userRepository.findByPublicId(id))
                .thenReturn(Optional.empty());
    }

    // =========================================================================
    // getMyProfile
    // =========================================================================

    @Nested
    @DisplayName("getMyProfile(UUID userId)")
    class GetMyProfileTests {

        @Test
        @DisplayName("returns a UserProfileResponse mapped from the found user")
        void whenUserExists_returnsProfileResponse() {
            givenUserExists(activeUser);

            UserProfileResponse response = userService.getMyProfile(userId);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(activeUser.getId());
            assertThat(response.getEmail()).isEqualTo(activeUser.getEmail());
            assertThat(response.getUsername()).isEqualTo(activeUser.getUsername());
            assertThat(response.getFirstName()).isEqualTo(activeUser.getFirstName());
            assertThat(response.getLastName()).isEqualTo(activeUser.getLastName());
            assertThat(response.getRole()).isEqualTo(activeUser.getRole());
            assertThat(response.getStatus()).isEqualTo(activeUser.getStatus());
        }

        @Test
        @DisplayName("propagates ResourceNotFoundException when the user does not exist")
        void whenUserNotFound_throwsResourceNotFoundException() {
            givenUserNotFound(userId);

            assertThatThrownBy(() -> userService.getMyProfile(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(userId.toString());
        }

        @Test
        @DisplayName("maps optional profile fields correctly (displayName, phone, avatarUrl, dates, prefs)")
        void mapsOptionalFieldsCorrectly() {
            User richUser = User.builder()
                    .id(userId)
                    .email("rich@example.com")
                    .username("rich")
                    .firstName("Rich")
                    .lastName("Guy")
                    .displayName("Richie")
                    .phone("+233501234567")
                    .dateOfBirth(LocalDate.of(1990, 5, 15))
                    .language("fr")
                    .timezone("Africa/Accra")
                    .currency("USD")
                    .role(Role.SELLER)
                    .status(UserStatus.ACTIVE)
                    .deletionRequestedAt(null)
                    .build();
            when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(richUser));

            UserProfileResponse response = userService.getMyProfile(userId);

            assertThat(response.getDisplayName()).isEqualTo("Richie");
            assertThat(response.getPhone()).isEqualTo("+233501234567");
            assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(response.getLanguage()).isEqualTo("fr");
            assertThat(response.getTimezone()).isEqualTo("Africa/Accra");
            assertThat(response.getCurrency()).isEqualTo("USD");
            assertThat(response.getRole()).isEqualTo(Role.SELLER);
        }
    }

    // =========================================================================
    // updateMyProfile
    // =========================================================================

    @Nested
    @DisplayName("updateMyProfile(UUID userId, UpdateProfileRequest request)")
    class UpdateMyProfileTests {

        private UpdateProfileRequest buildRequest(String firstName, String lastName,
                                                   String displayName, String phone,
                                                   String avatarUrl, LocalDate dob,
                                                   String language, String timezone,
                                                   String currency) {
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setFirstName(firstName);
            req.setLastName(lastName);
            req.setDisplayName(displayName);
            req.setPhone(phone);
            req.setAvatarUrl(avatarUrl);
            req.setDateOfBirth(dob);
            req.setLanguage(language);
            req.setTimezone(timezone);
            req.setCurrency(currency);
            return req;
        }

        @Test
        @DisplayName("updates all non-null fields on the user entity and saves")
        void whenAllFieldsProvided_updatesAndSaves() {
            givenUserExists(activeUser);

            UpdateProfileRequest request = buildRequest(
                    "Bob", "Jones", "Bobby", "+233991234567",
                    "https://cdn.example.com/avatar.png",
                    LocalDate.of(1995, 3, 10),
                    "en", "America/New_York", "EUR"
            );

            UserProfileResponse response = userService.updateMyProfile(userId, request);

            // Verify entity mutations
            assertThat(activeUser.getFirstName()).isEqualTo("Bob");
            assertThat(activeUser.getLastName()).isEqualTo("Jones");
            assertThat(activeUser.getDisplayName()).isEqualTo("Bobby");
            assertThat(activeUser.getPhone()).isEqualTo("+233991234567");
            assertThat(activeUser.getProfileImageUrl()).isEqualTo("https://cdn.example.com/avatar.png");
            assertThat(activeUser.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 3, 10));
            assertThat(activeUser.getLanguage()).isEqualTo("en");
            assertThat(activeUser.getTimezone()).isEqualTo("America/New_York");
            assertThat(activeUser.getCurrency()).isEqualTo("EUR");

            verify(userRepository).save(activeUser);
            verify(tokenValidationService).evictPrincipal(userId);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("skips null fields — does not overwrite existing values")
        void whenNullFieldsProvided_skipsUpdate() {
            activeUser.setFirstName("OriginalFirst");
            activeUser.setLastName("OriginalLast");
            givenUserExists(activeUser);

            UpdateProfileRequest request = buildRequest(
                    null, null, null, null, null, null, null, null, null
            );

            userService.updateMyProfile(userId, request);

            assertThat(activeUser.getFirstName()).isEqualTo("OriginalFirst");
            assertThat(activeUser.getLastName()).isEqualTo("OriginalLast");
            verify(userRepository).save(activeUser);
            verify(tokenValidationService).evictPrincipal(userId);
        }

        @Test
        @DisplayName("returns response with updated values reflected")
        void returnsResponseWithUpdatedValues() {
            givenUserExists(activeUser);

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("UpdatedName");

            UserProfileResponse response = userService.updateMyProfile(userId, request);

            assertThat(response.getFirstName()).isEqualTo("UpdatedName");
            assertThat(response.getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user does not exist")
        void whenUserNotFound_throwsException() {
            givenUserNotFound(userId);

            assertThatThrownBy(() -> userService.updateMyProfile(userId, new UpdateProfileRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any());
            verify(tokenValidationService, never()).evictPrincipal(any());
        }

        @Test
        @DisplayName("evicts principal cache on every successful update")
        void alwaysEvictsPrincipalOnSuccess() {
            givenUserExists(activeUser);

            userService.updateMyProfile(userId, new UpdateProfileRequest());

            verify(tokenValidationService).evictPrincipal(userId);
        }
    }

    // =========================================================================
    // deactivateAccount
    // =========================================================================

    @Nested
    @DisplayName("deactivateAccount(UUID userId, AccountDeactivationRequest request)")
    class DeactivateAccountTests {

        private AccountDeactivationRequest confirmedRequest() {
            AccountDeactivationRequest req = new AccountDeactivationRequest();
            req.setConfirm(true);
            req.setReason("No longer need the account");
            return req;
        }

        private AccountDeactivationRequest unconfirmedRequest() {
            AccountDeactivationRequest req = new AccountDeactivationRequest();
            req.setConfirm(false);
            req.setReason("Some reason");
            return req;
        }

        @Test
        @DisplayName("throws IllegalArgumentException when confirm is false")
        void whenConfirmFalse_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> userService.deactivateAccount(userId, unconfirmedRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Confirmation required");

            verifyNoInteractions(userRepository);
            verifyNoInteractions(tokenValidationService);
        }

        @Test
        @DisplayName("sets status to DISABLED, isActive to false, disabledAt timestamp, then saves")
        void whenConfirmed_deactivatesUserAndSaves() {
            givenUserExists(activeUser);

            userService.deactivateAccount(userId, confirmedRequest());

            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.DISABLED);
            assertThat(activeUser.getIsActive()).isFalse();
            assertThat(activeUser.getDisabledAt()).isNotNull();

            verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("evicts principal cache after successful deactivation")
        void evictsPrincipalAfterDeactivation() {
            givenUserExists(activeUser);

            userService.deactivateAccount(userId, confirmedRequest());

            verify(tokenValidationService).evictPrincipal(userId);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user does not exist")
        void whenUserNotFound_throwsResourceNotFoundException() {
            givenUserNotFound(userId);

            assertThatThrownBy(() -> userService.deactivateAccount(userId, confirmedRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("disabledAt is set to a recent Instant")
        void disabledAtIsSetToNow() {
            Instant before = Instant.now();
            givenUserExists(activeUser);

            userService.deactivateAccount(userId, confirmedRequest());

            Instant after = Instant.now();
            assertThat(activeUser.getDisabledAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }
    }

    // =========================================================================
    // requestAccountDeletion
    // =========================================================================

    @Nested
    @DisplayName("requestAccountDeletion(UUID userId, AccountDeletionRequest request)")
    class RequestAccountDeletionTests {

        private AccountDeletionRequest confirmedRequest() {
            AccountDeletionRequest req = new AccountDeletionRequest();
            req.setConfirm(true);
            req.setReason("Closing account");
            return req;
        }

        private AccountDeletionRequest unconfirmedRequest() {
            AccountDeletionRequest req = new AccountDeletionRequest();
            req.setConfirm(false);
            req.setReason("Closing account");
            return req;
        }

        @Test
        @DisplayName("throws IllegalArgumentException when confirm is false")
        void whenConfirmFalse_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> userService.requestAccountDeletion(userId, unconfirmedRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Confirmation required");

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("throws IllegalStateException when deletion request already pending")
        void whenDeletionAlreadyPending_throwsIllegalStateException() {
            activeUser.setDeletionRequestedAt(Instant.now().minusSeconds(3600));
            givenUserExists(activeUser);

            assertThatThrownBy(() -> userService.requestAccountDeletion(userId, confirmedRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Deletion request already pending");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("sets deletionRequestedAt, scheduledDeletionAt (+30 days), status DISABLED, isActive false")
        void whenConfirmedAndNoPendingDeletion_setsFieldsAndSaves() {
            givenUserExists(activeUser);
            Instant before = Instant.now();

            userService.requestAccountDeletion(userId, confirmedRequest());

            Instant after = Instant.now();

            assertThat(activeUser.getDeletionRequestedAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);

            assertThat(activeUser.getScheduledDeletionAt())
                    .isAfterOrEqualTo(activeUser.getDeletionRequestedAt().plusSeconds(29 * 24 * 3600))
                    .isBeforeOrEqualTo(after.plusSeconds(31L * 24 * 3600));

            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.DISABLED);
            assertThat(activeUser.getIsActive()).isFalse();

            verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("evicts principal cache after successful deletion request")
        void evictsPrincipalAfterDeletionRequest() {
            givenUserExists(activeUser);

            userService.requestAccountDeletion(userId, confirmedRequest());

            verify(tokenValidationService).evictPrincipal(userId);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user not found")
        void whenUserNotFound_throwsResourceNotFoundException() {
            givenUserNotFound(userId);

            assertThatThrownBy(() -> userService.requestAccountDeletion(userId, confirmedRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // adminSearchUsers
    // =========================================================================

    @Nested
    @DisplayName("adminSearchUsers(AdminUserSearchParams params, Pageable pageable)")
    class AdminSearchUsersTests {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("returns a Page of UserProfileResponse for a query with no filters")
        void whenNoFilters_returnsPageOfProfiles() {
            AdminUserSearchParams params = new AdminUserSearchParams();
            params.setQuery("alice");

            Page<User> repoPage = new PageImpl<>(List.of(activeUser), pageable, 1);
            when(userRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(repoPage);

            Page<UserProfileResponse> result = userService.adminSearchUsers(params, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(activeUser.getId());
        }

        @Test
        @DisplayName("applies status filter when params.status is set")
        void whenStatusFilterSet_passesSpecificationToRepository() {
            AdminUserSearchParams params = new AdminUserSearchParams();
            params.setStatus(UserStatus.SUSPENDED);

            Page<User> repoPage = new PageImpl<>(List.of(), pageable, 0);
            when(userRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(repoPage);

            Page<UserProfileResponse> result = userService.adminSearchUsers(params, pageable);

            assertThat(result.getTotalElements()).isZero();
            verify(userRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("applies role filter when params.role is set")
        void whenRoleFilterSet_passesSpecificationToRepository() {
            AdminUserSearchParams params = new AdminUserSearchParams();
            params.setRole(Role.ADMIN);

            Page<User> repoPage = new PageImpl<>(List.of(), pageable, 0);
            when(userRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(repoPage);

            Page<UserProfileResponse> result = userService.adminSearchUsers(params, pageable);

            assertThat(result).isNotNull();
            verify(userRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("applies emailVerified filter when params.emailVerified is set")
        void whenEmailVerifiedFilterSet_passesSpecificationToRepository() {
            AdminUserSearchParams params = new AdminUserSearchParams();
            params.setEmailVerified(true);

            Page<User> repoPage = new PageImpl<>(List.of(activeUser), pageable, 1);
            when(userRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(repoPage);

            Page<UserProfileResponse> result = userService.adminSearchUsers(params, pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("maps returned users to UserProfileResponse correctly")
        void mapsUserToProfileResponseCorrectly() {
            AdminUserSearchParams params = new AdminUserSearchParams();

            Page<User> repoPage = new PageImpl<>(List.of(activeUser), pageable, 1);
            when(userRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(repoPage);

            Page<UserProfileResponse> result = userService.adminSearchUsers(params, pageable);

            UserProfileResponse mapped = result.getContent().get(0);
            assertThat(mapped.getEmail()).isEqualTo(activeUser.getEmail());
            assertThat(mapped.getUsername()).isEqualTo(activeUser.getUsername());
            assertThat(mapped.getStatus()).isEqualTo(activeUser.getStatus());
            assertThat(mapped.getRole()).isEqualTo(activeUser.getRole());
        }

        @Test
        @DisplayName("returns empty page when repository returns no results")
        void whenRepositoryReturnsEmpty_returnsEmptyPage() {
            AdminUserSearchParams params = new AdminUserSearchParams();

            Page<User> repoPage = new PageImpl<>(List.of(), pageable, 0);
            when(userRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(repoPage);

            Page<UserProfileResponse> result = userService.adminSearchUsers(params, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // =========================================================================
    // adminGetUser
    // =========================================================================

    @Nested
    @DisplayName("adminGetUser(UUID userId)")
    class AdminGetUserTests {

        @Test
        @DisplayName("returns UserProfileResponse for an existing user")
        void whenUserExists_returnsProfileResponse() {
            givenUserExists(activeUser);

            UserProfileResponse response = userService.adminGetUser(userId);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(activeUser.getId());
            assertThat(response.getEmail()).isEqualTo(activeUser.getEmail());
            assertThat(response.getStatus()).isEqualTo(activeUser.getStatus());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user does not exist")
        void whenUserNotFound_throwsResourceNotFoundException() {
            givenUserNotFound(userId);

            assertThatThrownBy(() -> userService.adminGetUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(userId.toString());
        }

        @Test
        @DisplayName("maps all profile fields into the response")
        void mapsAllFieldsIntoResponse() {
            activeUser.setRole(Role.ADMIN);
            activeUser.setStatus(UserStatus.SUSPENDED);
            givenUserExists(activeUser);

            UserProfileResponse response = userService.adminGetUser(userId);

            assertThat(response.getRole()).isEqualTo(Role.ADMIN);
            assertThat(response.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(response.getUsername()).isEqualTo("alice");
        }
    }

    // =========================================================================
    // adminSuspendUser
    // =========================================================================

    @Nested
    @DisplayName("adminSuspendUser(UUID targetId, UUID actorId, AdminSuspendRequest request)")
    class AdminSuspendUserTests {

        private UUID actorId;
        private AdminSuspendRequest suspendRequest;

        @BeforeEach
        void setUp() {
            actorId = UUID.randomUUID();
            suspendRequest = new AdminSuspendRequest();
            suspendRequest.setReason("Violating ToS");
            suspendRequest.setDurationDays(7);
        }

        @Test
        @DisplayName("throws IllegalStateException when user is already SUSPENDED")
        void whenUserAlreadySuspended_throwsIllegalStateException() {
            activeUser.setStatus(UserStatus.SUSPENDED);
            givenUserExists(activeUser);

            assertThatThrownBy(() -> userService.adminSuspendUser(userId, actorId, suspendRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already suspended");

            verify(userRepository, never()).save(any());
            verify(tokenValidationService, never()).evictPrincipal(any());
        }

        @Test
        @DisplayName("sets status to SUSPENDED, reason, suspendedBy, suspendedAt and isActive=false")
        void whenUserActive_suspendUserAndSave() {
            givenUserExists(activeUser);
            Instant before = Instant.now();

            userService.adminSuspendUser(userId, actorId, suspendRequest);

            Instant after = Instant.now();

            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(activeUser.getSuspendReason()).isEqualTo("Violating ToS");
            assertThat(activeUser.getSuspendedBy()).isEqualTo(actorId);
            assertThat(activeUser.getIsActive()).isFalse();
            assertThat(activeUser.getSuspendedAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);

            verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("evicts principal cache after suspension")
        void evictsPrincipalAfterSuspension() {
            givenUserExists(activeUser);

            userService.adminSuspendUser(userId, actorId, suspendRequest);

            verify(tokenValidationService).evictPrincipal(userId);
        }

        @Test
        @DisplayName("returns UserProfileResponse reflecting the new SUSPENDED status")
        void returnsProfileResponseWithSuspendedStatus() {
            givenUserExists(activeUser);

            UserProfileResponse response = userService.adminSuspendUser(userId, actorId, suspendRequest);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(response.getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when target user does not exist")
        void whenTargetUserNotFound_throwsResourceNotFoundException() {
            givenUserNotFound(userId);

            assertThatThrownBy(() -> userService.adminSuspendUser(userId, actorId, suspendRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("can suspend a DISABLED user (only SUSPENDED blocks re-suspension)")
        void canSuspendDisabledUser() {
            activeUser.setStatus(UserStatus.DISABLED);
            givenUserExists(activeUser);

            userService.adminSuspendUser(userId, actorId, suspendRequest);

            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            verify(userRepository).save(activeUser);
        }
    }

    // =========================================================================
    // adminActivateUser
    // =========================================================================

    @Nested
    @DisplayName("adminActivateUser(UUID targetId)")
    class AdminActivateUserTests {

        @Test
        @DisplayName("sets status to ACTIVE, isActive=true, clears suspension/disable fields")
        void whenUserSuspended_activatesAndClearsFields() {
            activeUser.setStatus(UserStatus.SUSPENDED);
            activeUser.setIsActive(false);
            activeUser.setSuspendReason("Violating ToS");
            activeUser.setSuspendedBy(UUID.randomUUID());
            activeUser.setSuspendedAt(Instant.now().minusSeconds(3600));
            activeUser.setDisabledAt(Instant.now().minusSeconds(7200));
            givenUserExists(activeUser);

            userService.adminActivateUser(userId);

            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(activeUser.getIsActive()).isTrue();
            assertThat(activeUser.getSuspendReason()).isNull();
            assertThat(activeUser.getSuspendedBy()).isNull();
            assertThat(activeUser.getSuspendedAt()).isNull();
            assertThat(activeUser.getDisabledAt()).isNull();

            verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("returns UserProfileResponse reflecting ACTIVE status")
        void returnsProfileResponseWithActiveStatus() {
            activeUser.setStatus(UserStatus.DISABLED);
            activeUser.setIsActive(false);
            givenUserExists(activeUser);

            UserProfileResponse response = userService.adminActivateUser(userId);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(response.getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("does not evict principal cache — activation does not invalidate sessions")
        void doesNotEvictPrincipalOnActivation() {
            givenUserExists(activeUser);

            userService.adminActivateUser(userId);

            verify(tokenValidationService, never()).evictPrincipal(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user does not exist")
        void whenUserNotFound_throwsResourceNotFoundException() {
            givenUserNotFound(userId);

            assertThatThrownBy(() -> userService.adminActivateUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("can activate an already-active user (idempotent save)")
        void canActivateAlreadyActiveUser() {
            // No guard in the service — save is always called
            givenUserExists(activeUser);

            userService.adminActivateUser(userId);

            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(activeUser.getIsActive()).isTrue();
            verify(userRepository).save(activeUser);
        }
    }

    // =========================================================================
    // adminDisableUser
    // =========================================================================

    @Nested
    @DisplayName("adminDisableUser(UUID targetId, String reason)")
    class AdminDisableUserTests {

        @Test
        @DisplayName("sets status to DISABLED, isActive=false, disabledAt timestamp, and saves")
        void whenUserActive_disablesAndSaves() {
            givenUserExists(activeUser);
            Instant before = Instant.now();

            userService.adminDisableUser(userId, "Abuse detected");

            Instant after = Instant.now();

            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.DISABLED);
            assertThat(activeUser.getIsActive()).isFalse();
            assertThat(activeUser.getDisabledAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);

            verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("stores provided reason in suspendReason when reason is non-null")
        void whenReasonProvided_storesSuspendReason() {
            givenUserExists(activeUser);

            userService.adminDisableUser(userId, "Repeated policy violations");

            assertThat(activeUser.getSuspendReason()).isEqualTo("Repeated policy violations");
        }

        @Test
        @DisplayName("does not set suspendReason when reason is null")
        void whenReasonNull_doesNotSetSuspendReason() {
            activeUser.setSuspendReason(null);
            givenUserExists(activeUser);

            userService.adminDisableUser(userId, null);

            assertThat(activeUser.getSuspendReason()).isNull();
            verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("evicts principal cache after disabling")
        void evictsPrincipalAfterDisabling() {
            givenUserExists(activeUser);

            userService.adminDisableUser(userId, "Policy breach");

            verify(tokenValidationService).evictPrincipal(userId);
        }

        @Test
        @DisplayName("returns UserProfileResponse reflecting the new DISABLED status")
        void returnsProfileResponseWithDisabledStatus() {
            givenUserExists(activeUser);

            UserProfileResponse response = userService.adminDisableUser(userId, "Admin decision");

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(UserStatus.DISABLED);
            assertThat(response.getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user does not exist")
        void whenUserNotFound_throwsResourceNotFoundException() {
            givenUserNotFound(userId);

            assertThatThrownBy(() -> userService.adminDisableUser(userId, "Some reason"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any());
            verify(tokenValidationService, never()).evictPrincipal(any());
        }

        @Test
        @DisplayName("can disable an already-disabled user — save is always called (no guard)")
        void canDisableAlreadyDisabledUser() {
            activeUser.setStatus(UserStatus.DISABLED);
            activeUser.setIsActive(false);
            givenUserExists(activeUser);

            userService.adminDisableUser(userId, "Redundant disable");

            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.DISABLED);
            verify(userRepository).save(activeUser);
        }
    }
}
