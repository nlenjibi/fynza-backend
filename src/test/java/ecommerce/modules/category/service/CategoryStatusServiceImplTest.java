package ecommerce.modules.category.service;

import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.category.dto.request.CategoryStatusRequest;
import ecommerce.modules.category.dto.response.CategoryResponse;
import ecommerce.modules.category.dto.response.CategoryStatusHistoryResponse;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.entity.CategoryStatusHistory;
import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.exception.CategoryNotFoundException;
import ecommerce.modules.category.exception.CategoryStatusTransitionException;
import ecommerce.modules.category.mapper.CategoryMapper;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.category.repository.CategoryStatusHistoryRepository;
import ecommerce.modules.category.service.impl.CategoryStatusServiceImpl;
import ecommerce.modules.category.validation.CategoryStatusTransitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryStatusServiceImpl Tests")
class CategoryStatusServiceImplTest {

    @Mock private CategoryRepository              categoryRepository;
    @Mock private CategoryStatusHistoryRepository historyRepository;
    @Mock private CategoryStatusTransitionValidator transitionValidator;
    @Mock private CategoryMapper                  mapper;
    @Mock private AuditLogService                 auditLogService;

    @InjectMocks
    private CategoryStatusServiceImpl service;

    private UUID categoryPublicId;
    private UUID actorId;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryPublicId = UUID.randomUUID();
        actorId          = UUID.randomUUID();

        category = Category.builder()
                .name("Electronics").slug("electronics")
                .status(CategoryStatus.DRAFT).isActive(true).build();
        setId(category, 1L);
        setPublicId(category, categoryPublicId);

        when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(historyRepository.save(any(CategoryStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Category.class))).thenReturn(CategoryResponse.builder().build());
    }

    @Nested
    @DisplayName("changeStatus(categoryPublicId, request, actorUserId)")
    class ChangeStatus {

        @Test
        @DisplayName("Invalid transition — validator throws, nothing saved")
        void changeStatus_invalidTransition_throwsCategoryStatusTransitionException() {
            CategoryStatusRequest request = CategoryStatusRequest.builder()
                    .status(CategoryStatus.ARCHIVED).build();

            doThrow(new CategoryStatusTransitionException(CategoryStatus.DRAFT, CategoryStatus.ARCHIVED))
                    .when(transitionValidator).validate(CategoryStatus.DRAFT, CategoryStatus.ARCHIVED);

            assertThatThrownBy(() -> service.changeStatus(categoryPublicId, request, actorId))
                    .isInstanceOf(CategoryStatusTransitionException.class)
                    .hasMessageContaining("DRAFT")
                    .hasMessageContaining("ARCHIVED");

            verify(historyRepository, never()).save(any());
            verify(categoryRepository, never()).save(any());
            verify(auditLogService, never()).log(any());
        }

        @Test
        @DisplayName("Valid transition — saves history with correct fields, updates entity, logs audit")
        void changeStatus_validTransition_savesHistoryUpdatesEntityAndLogsAudit() {
            CategoryStatusRequest request = CategoryStatusRequest.builder()
                    .status(CategoryStatus.ACTIVE).reason("Ready for publish").build();

            service.changeStatus(categoryPublicId, request, actorId);

            assertThat(category.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
            verify(categoryRepository).save(category);

            ArgumentCaptor<CategoryStatusHistory> histCaptor =
                    ArgumentCaptor.forClass(CategoryStatusHistory.class);
            verify(historyRepository).save(histCaptor.capture());
            CategoryStatusHistory history = histCaptor.getValue();
            assertThat(history.getPreviousStatus()).isEqualTo(CategoryStatus.DRAFT);
            assertThat(history.getNewStatus()).isEqualTo(CategoryStatus.ACTIVE);
            assertThat(history.getReason()).isEqualTo("Ready for publish");
            assertThat(history.getChangedBy()).isEqualTo(actorId);
            assertThat(history.getCategoryId()).isEqualTo(1L);

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).log(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getActorPublicId()).isEqualTo(actorId);
            assertThat(auditCaptor.getValue().getEntityPublicId()).isEqualTo(categoryPublicId);
        }

        @Test
        @DisplayName("Transition to ACTIVE sets isActive to true")
        void changeStatus_toActive_setsIsActiveTrue() {
            category.setStatus(CategoryStatus.INACTIVE);
            category.setIsActive(false);

            service.changeStatus(categoryPublicId,
                    CategoryStatusRequest.builder().status(CategoryStatus.ACTIVE).build(), actorId);

            assertThat(category.getIsActive()).isTrue();
            assertThat(category.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
        }

        @Test
        @DisplayName("Transition to INACTIVE sets isActive to false")
        void changeStatus_toInactive_setsIsActiveFalse() {
            category.setStatus(CategoryStatus.ACTIVE);
            category.setIsActive(true);

            service.changeStatus(categoryPublicId,
                    CategoryStatusRequest.builder().status(CategoryStatus.INACTIVE).build(), actorId);

            assertThat(category.getIsActive()).isFalse();
            assertThat(category.getStatus()).isEqualTo(CategoryStatus.INACTIVE);
        }

        @Test
        @DisplayName("Throws CategoryNotFoundException when category publicId not found")
        void changeStatus_categoryNotFound_throwsCategoryNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(categoryRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeStatus(unknownId,
                    CategoryStatusRequest.builder().status(CategoryStatus.ACTIVE).build(), actorId))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("getStatusHistory(categoryPublicId)")
    class GetStatusHistory {

        @Test
        @DisplayName("Returns mapped list of history responses")
        void getStatusHistory_returnsMappedList() {
            CategoryStatusHistory h1 = CategoryStatusHistory.builder()
                    .categoryId(1L).previousStatus(CategoryStatus.DRAFT)
                    .newStatus(CategoryStatus.ACTIVE).changedBy(actorId).build();
            CategoryStatusHistory h2 = CategoryStatusHistory.builder()
                    .categoryId(1L).previousStatus(CategoryStatus.ACTIVE)
                    .newStatus(CategoryStatus.INACTIVE).changedBy(actorId).build();

            when(historyRepository.findByCategoryIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(h1, h2));

            CategoryStatusHistoryResponse r1 = CategoryStatusHistoryResponse.builder()
                    .previousStatus(CategoryStatus.DRAFT).newStatus(CategoryStatus.ACTIVE).build();
            CategoryStatusHistoryResponse r2 = CategoryStatusHistoryResponse.builder()
                    .previousStatus(CategoryStatus.ACTIVE).newStatus(CategoryStatus.INACTIVE).build();
            when(mapper.toHistoryResponse(h1)).thenReturn(r1);
            when(mapper.toHistoryResponse(h2)).thenReturn(r2);

            List<CategoryStatusHistoryResponse> result = service.getStatusHistory(categoryPublicId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getNewStatus()).isEqualTo(CategoryStatus.ACTIVE);
            assertThat(result.get(1).getNewStatus()).isEqualTo(CategoryStatus.INACTIVE);
        }

        @Test
        @DisplayName("Returns empty list when no history exists")
        void getStatusHistory_noHistory_returnsEmptyList() {
            when(historyRepository.findByCategoryIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            assertThat(service.getStatusHistory(categoryPublicId)).isEmpty();
        }

        @Test
        @DisplayName("Throws CategoryNotFoundException when category publicId not found")
        void getStatusHistory_categoryNotFound_throwsCategoryNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(categoryRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStatusHistory(unknownId))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    private static void setId(Category c, Long id) {
        try {
            java.lang.reflect.Field f = Category.class.getDeclaredField("id");
            f.setAccessible(true); f.set(c, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void setPublicId(Category c, UUID publicId) {
        try {
            java.lang.reflect.Field f = Category.class.getDeclaredField("publicId");
            f.setAccessible(true); f.set(c, publicId);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
