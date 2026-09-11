package ecommerce.modules.category.service;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.category.dto.request.CategorySuggestionRequest;
import ecommerce.modules.category.dto.response.CategorySuggestionResponse;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.entity.CategorySuggestion;
import ecommerce.modules.category.enums.CategorySuggestionStatus;
import ecommerce.modules.category.exception.CategoryNotFoundException;
import ecommerce.modules.category.mapper.CategoryMapper;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.category.repository.CategorySuggestionRepository;
import ecommerce.modules.category.service.impl.CategorySuggestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategorySuggestionServiceImpl Tests")
class CategorySuggestionServiceImplTest {

    @Mock private CategorySuggestionRepository suggestionRepository;
    @Mock private CategoryRepository           categoryRepository;
    @Mock private CategoryMapper               mapper;
    @Mock private AuditLogService              auditLogService;

    @InjectMocks
    private CategorySuggestionServiceImpl service;

    private UUID requestedBy;
    private UUID suggestionPublicId;
    private CategorySuggestion suggestion;

    @BeforeEach
    void setUp() {
        requestedBy        = UUID.randomUUID();
        suggestionPublicId = UUID.randomUUID();

        suggestion = CategorySuggestion.builder()
                .requestedBy(requestedBy).name("Accessories")
                .description("Nice accessories").reason("Market demand")
                .status(CategorySuggestionStatus.PENDING).build();
        setSuggestionPublicId(suggestion, suggestionPublicId);

        when(suggestionRepository.findByPublicId(suggestionPublicId)).thenReturn(Optional.of(suggestion));
        when(suggestionRepository.save(any(CategorySuggestion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toSuggestionResponse(any(CategorySuggestion.class)))
                .thenReturn(CategorySuggestionResponse.builder()
                        .publicId(suggestionPublicId).name("Accessories")
                        .status(CategorySuggestionStatus.PENDING).build());
    }

    @Nested
    @DisplayName("submitSuggestion(request, requestedBy)")
    class SubmitSuggestion {

        @Test
        @DisplayName("With parent — resolves parentCategoryId from publicId")
        void submitSuggestion_withParent_setsParentCategoryId() {
            UUID parentPublicId = UUID.randomUUID();
            Category parent = Category.builder().name("Tech").slug("tech").build();
            setId(parent, 5L);
            setPublicId(parent, parentPublicId);

            when(categoryRepository.findByPublicId(parentPublicId)).thenReturn(Optional.of(parent));
            when(suggestionRepository.save(any(CategorySuggestion.class))).thenAnswer(inv -> {
                CategorySuggestion s = inv.getArgument(0);
                setSuggestionPublicId(s, suggestionPublicId);
                return s;
            });

            service.submitSuggestion(CategorySuggestionRequest.builder()
                    .name("Phones").parentCategoryPublicId(parentPublicId).build(), requestedBy);

            ArgumentCaptor<CategorySuggestion> captor = ArgumentCaptor.forClass(CategorySuggestion.class);
            verify(suggestionRepository).save(captor.capture());
            assertThat(captor.getValue().getParentCategoryId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Without parent — parentCategoryId is null on saved suggestion")
        void submitSuggestion_withoutParent_parentCategoryIdIsNull() {
            when(suggestionRepository.save(any(CategorySuggestion.class))).thenAnswer(inv -> {
                CategorySuggestion s = inv.getArgument(0);
                setSuggestionPublicId(s, suggestionPublicId);
                return s;
            });

            service.submitSuggestion(CategorySuggestionRequest.builder()
                    .name("Phones").parentCategoryPublicId(null).build(), requestedBy);

            ArgumentCaptor<CategorySuggestion> captor = ArgumentCaptor.forClass(CategorySuggestion.class);
            verify(suggestionRepository).save(captor.capture());
            assertThat(captor.getValue().getParentCategoryId()).isNull();
            verify(categoryRepository, never()).findByPublicId(any());
        }

        @Test
        @DisplayName("Audit uses saved entity publicId (not pre-save null)")
        void submitSuggestion_auditEntityPublicId_usesSavedPublicId() {
            UUID savedId = UUID.randomUUID();
            when(suggestionRepository.save(any(CategorySuggestion.class))).thenAnswer(inv -> {
                CategorySuggestion s = inv.getArgument(0);
                setSuggestionPublicId(s, savedId);
                return s;
            });

            service.submitSuggestion(CategorySuggestionRequest.builder()
                    .name("Accessories").build(), requestedBy);

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).log(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getEntityPublicId()).isEqualTo(savedId);
            assertThat(auditCaptor.getValue().getActorPublicId()).isEqualTo(requestedBy);
        }

        @Test
        @DisplayName("Parent publicId not found — throws CategoryNotFoundException")
        void submitSuggestion_parentNotFound_throwsCategoryNotFoundException() {
            UUID unknownParentId = UUID.randomUUID();
            when(categoryRepository.findByPublicId(unknownParentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitSuggestion(CategorySuggestionRequest.builder()
                    .name("Phones").parentCategoryPublicId(unknownParentId).build(), requestedBy))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(suggestionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getSuggestions(status, pageable)")
    class GetSuggestions {

        @Test
        @DisplayName("With status filter — delegates to findByStatus")
        void getSuggestions_withStatus_usesFilteredQuery() {
            Pageable pageable = PageRequest.of(0, 10);
            when(suggestionRepository.findByStatus(CategorySuggestionStatus.PENDING, pageable))
                    .thenReturn(new PageImpl<>(List.of(suggestion)));

            Page<CategorySuggestionResponse> result =
                    service.getSuggestions(CategorySuggestionStatus.PENDING, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(suggestionRepository).findByStatus(CategorySuggestionStatus.PENDING, pageable);
            verify(suggestionRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Without status (null) — delegates to findAll")
        void getSuggestions_withoutStatus_usesFullScan() {
            Pageable pageable = PageRequest.of(0, 10);
            when(suggestionRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(suggestion)));

            Page<CategorySuggestionResponse> result = service.getSuggestions(null, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(suggestionRepository).findAll(pageable);
            verify(suggestionRepository, never()).findByStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("getMySuggestions(requestedBy, pageable)")
    class GetMySuggestions {

        @Test
        @DisplayName("Delegates to findByRequestedBy and maps results")
        void getMySuggestions_delegatesToRepo_returnsMappedPage() {
            Pageable pageable = PageRequest.of(0, 5);
            when(suggestionRepository.findByRequestedBy(requestedBy, pageable))
                    .thenReturn(new PageImpl<>(List.of(suggestion)));

            Page<CategorySuggestionResponse> result = service.getMySuggestions(requestedBy, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(suggestionRepository).findByRequestedBy(requestedBy, pageable);
        }
    }

    @Nested
    @DisplayName("approveSuggestion(suggestionPublicId, reviewedBy)")
    class ApproveSuggestion {

        @Test
        @DisplayName("Sets status APPROVED, reviewedBy, reviewedAt non-null, saves and logs audit")
        void approveSuggestion_happyPath_setsApprovedFields() {
            UUID reviewedBy = UUID.randomUUID();

            service.approveSuggestion(suggestionPublicId, reviewedBy);

            assertThat(suggestion.getStatus()).isEqualTo(CategorySuggestionStatus.APPROVED);
            assertThat(suggestion.getReviewedBy()).isEqualTo(reviewedBy);
            assertThat(suggestion.getReviewedAt()).isNotNull();
            verify(suggestionRepository).save(suggestion);

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).log(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getActorPublicId()).isEqualTo(reviewedBy);
            assertThat(auditCaptor.getValue().getEntityPublicId()).isEqualTo(suggestionPublicId);
        }

        @Test
        @DisplayName("Suggestion not found — throws ResourceNotFoundException")
        void approveSuggestion_notFound_throwsResourceNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(suggestionRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approveSuggestion(unknownId, UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("rejectSuggestion(suggestionPublicId, reason, reviewedBy)")
    class RejectSuggestion {

        @Test
        @DisplayName("Sets REJECTED, reviewedBy, reviewedAt, and updates reason when provided")
        void rejectSuggestion_withReason_updatesAllFields() {
            UUID reviewedBy = UUID.randomUUID();

            service.rejectSuggestion(suggestionPublicId, "Not enough demand", reviewedBy);

            assertThat(suggestion.getStatus()).isEqualTo(CategorySuggestionStatus.REJECTED);
            assertThat(suggestion.getReviewedBy()).isEqualTo(reviewedBy);
            assertThat(suggestion.getReviewedAt()).isNotNull();
            assertThat(suggestion.getReason()).isEqualTo("Not enough demand");
            verify(suggestionRepository).save(suggestion);
        }

        @Test
        @DisplayName("Null reason — original reason not overwritten")
        void rejectSuggestion_nullReason_doesNotOverwriteExistingReason() {
            suggestion.setReason("original reason");
            UUID reviewedBy = UUID.randomUUID();

            service.rejectSuggestion(suggestionPublicId, null, reviewedBy);

            assertThat(suggestion.getStatus()).isEqualTo(CategorySuggestionStatus.REJECTED);
            assertThat(suggestion.getReason()).isEqualTo("original reason");
        }

        @Test
        @DisplayName("Suggestion not found — throws ResourceNotFoundException")
        void rejectSuggestion_notFound_throwsResourceNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(suggestionRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.rejectSuggestion(unknownId, "bad", UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private static void setSuggestionPublicId(CategorySuggestion s, UUID publicId) {
        try {
            java.lang.reflect.Field f = CategorySuggestion.class.getDeclaredField("publicId");
            f.setAccessible(true); f.set(s, publicId);
        } catch (Exception e) { throw new RuntimeException(e); }
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
