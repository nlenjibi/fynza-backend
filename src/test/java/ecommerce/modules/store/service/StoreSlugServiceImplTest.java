package ecommerce.modules.store.service;

import ecommerce.modules.store.entity.StoreSlugHistory;
import ecommerce.modules.store.exception.StoreSlugAlreadyTakenException;
import ecommerce.modules.store.repository.StoreRepository;
import ecommerce.modules.store.repository.StoreSlugHistoryRepository;
import ecommerce.modules.store.service.impl.StoreSlugServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreSlugServiceImpl Tests")
class StoreSlugServiceImplTest {

    @Mock private StoreRepository             storeRepository;
    @Mock private StoreSlugHistoryRepository  slugHistoryRepository;

    @InjectMocks
    private StoreSlugServiceImpl service;

    // -- generateSlug() --

    @Nested
    @DisplayName("generateSlug(storeName)")
    class GenerateSlug {

        @Test
        @DisplayName("Converts store name to lowercase hyphenated slug")
        void generateSlug_plainName_returnsHyphenatedSlug() {
            when(storeRepository.existsBySlug("my-store")).thenReturn(false);
            when(slugHistoryRepository.existsBySlug("my-store")).thenReturn(false);

            String result = service.generateSlug("My Store");

            assertThat(result).isEqualTo("my-store");
        }

        @Test
        @DisplayName("Strips special characters from store name")
        void generateSlug_specialChars_stripsAndHyphenates() {
            when(storeRepository.existsBySlug("my-store")).thenReturn(false);
            when(slugHistoryRepository.existsBySlug("my-store")).thenReturn(false);

            String result = service.generateSlug("My Store!");

            assertThat(result).isEqualTo("my-store");
        }

        @Test
        @DisplayName("Appends -2 suffix when base slug exists in storeRepository")
        void generateSlug_baseSlugExistsInStore_returnsSlugWithSuffix() {
            // storeRepository short-circuits the && condition; slugHistoryRepository.existsBySlug("my-store")
            // is NOT called due to the OR short-circuit in the do-while loop entry.
            when(storeRepository.existsBySlug("my-store")).thenReturn(true);
            // For the do-while candidate "my-store-2"
            when(storeRepository.existsBySlug("my-store-2")).thenReturn(false);
            when(slugHistoryRepository.existsBySlug("my-store-2")).thenReturn(false);

            String result = service.generateSlug("My Store");

            assertThat(result).isEqualTo("my-store-2");
        }

        @Test
        @DisplayName("Appends -2 suffix when base slug exists in slugHistoryRepository")
        void generateSlug_baseSlugExistsInHistory_returnsSlugWithSuffix() {
            when(storeRepository.existsBySlug("my-store")).thenReturn(false);
            when(slugHistoryRepository.existsBySlug("my-store")).thenReturn(true);
            when(storeRepository.existsBySlug("my-store-2")).thenReturn(false);
            when(slugHistoryRepository.existsBySlug("my-store-2")).thenReturn(false);

            String result = service.generateSlug("My Store");

            assertThat(result).isEqualTo("my-store-2");
        }

        @Test
        @DisplayName("Increments suffix further when -2 candidate is also taken")
        void generateSlug_multipleSuffixes_incrementsUntilFree() {
            // "my-store": storeRepo=true → short-circuits the initial if-check; historyRepo NOT called
            when(storeRepository.existsBySlug("my-store")).thenReturn(true);
            // do-while: "my-store-2": storeRepo=true → OR short-circuit; historyRepo NOT called
            when(storeRepository.existsBySlug("my-store-2")).thenReturn(true);
            // do-while: "my-store-3": storeRepo=false → historyRepo IS called
            when(storeRepository.existsBySlug("my-store-3")).thenReturn(false);
            when(slugHistoryRepository.existsBySlug("my-store-3")).thenReturn(false);

            String result = service.generateSlug("My Store");

            assertThat(result).isEqualTo("my-store-3");
        }

        @Test
        @DisplayName("Handles multi-word store names with multiple spaces")
        void generateSlug_multiWordName_collapsesSpaces() {
            when(storeRepository.existsBySlug("my-awesome-store")).thenReturn(false);
            when(slugHistoryRepository.existsBySlug("my-awesome-store")).thenReturn(false);

            String result = service.generateSlug("My Awesome Store");

            assertThat(result).isEqualTo("my-awesome-store");
        }
    }

    // -- validateAndReserve() --

    @Nested
    @DisplayName("validateAndReserve(slug, storeId)")
    class ValidateAndReserve {

        @Test
        @DisplayName("Saves StoreSlugHistory when slug is available")
        void validateAndReserve_availableSlug_savesHistory() {
            when(storeRepository.existsBySlug("new-slug")).thenReturn(false);
            when(slugHistoryRepository.existsBySlug("new-slug")).thenReturn(false);

            ArgumentCaptor<StoreSlugHistory> captor = ArgumentCaptor.forClass(StoreSlugHistory.class);
            when(slugHistoryRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.validateAndReserve("new-slug", 0L);

            StoreSlugHistory saved = captor.getValue();
            assertThat(saved.getSlug()).isEqualTo("new-slug");
            assertThat(saved.getStoreId()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Throws StoreSlugAlreadyTakenException when slug exists in storeRepository")
        void validateAndReserve_slugExistsInStore_throwsException() {
            when(storeRepository.existsBySlug("taken-slug")).thenReturn(true);

            assertThatThrownBy(() -> service.validateAndReserve("taken-slug", 0L))
                    .isInstanceOf(StoreSlugAlreadyTakenException.class)
                    .hasMessageContaining("taken-slug");
        }

        @Test
        @DisplayName("Throws StoreSlugAlreadyTakenException when slug exists in slugHistoryRepository")
        void validateAndReserve_slugExistsInHistory_throwsException() {
            when(storeRepository.existsBySlug("reserved-slug")).thenReturn(false);
            when(slugHistoryRepository.existsBySlug("reserved-slug")).thenReturn(true);

            assertThatThrownBy(() -> service.validateAndReserve("reserved-slug", 0L))
                    .isInstanceOf(StoreSlugAlreadyTakenException.class)
                    .hasMessageContaining("reserved-slug");
        }
    }

    // -- rotateSlug() --

    @Nested
    @DisplayName("rotateSlug(storeId, newSlug)")
    class RotateSlug {

        @Test
        @DisplayName("Saves StoreSlugHistory for the new slug")
        void rotateSlug_availableSlug_savesHistory() {
            when(storeRepository.existsBySlug("new-slug")).thenReturn(false);

            ArgumentCaptor<StoreSlugHistory> captor = ArgumentCaptor.forClass(StoreSlugHistory.class);
            when(slugHistoryRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.rotateSlug(42L, "new-slug");

            StoreSlugHistory saved = captor.getValue();
            assertThat(saved.getSlug()).isEqualTo("new-slug");
            assertThat(saved.getStoreId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("Throws StoreSlugAlreadyTakenException when new slug already exists in storeRepository")
        void rotateSlug_slugExists_throwsException() {
            when(storeRepository.existsBySlug("existing-slug")).thenReturn(true);

            assertThatThrownBy(() -> service.rotateSlug(42L, "existing-slug"))
                    .isInstanceOf(StoreSlugAlreadyTakenException.class)
                    .hasMessageContaining("existing-slug");
        }
    }
}
