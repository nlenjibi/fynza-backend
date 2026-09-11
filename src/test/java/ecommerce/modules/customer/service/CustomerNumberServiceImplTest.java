package ecommerce.modules.customer.service;

import ecommerce.modules.customer.service.impl.CustomerNumberServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerNumberServiceImpl Tests")
class CustomerNumberServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query nativeQuery;

    @InjectMocks
    private CustomerNumberServiceImpl service;

    @BeforeEach
    void setUp() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
    }

    // ── generate() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("Returns formatted customer number with CUS- prefix and zero-padded 6-digit sequence")
        void generate_formatsSequenceWithCUSPrefix() {
            when(nativeQuery.getSingleResult()).thenReturn(42L);

            String result = service.generate();

            assertThat(result).isEqualTo("CUS-000042");
        }

        @Test
        @DisplayName("Zero-pads sequence value to 6 digits")
        void generate_zeroPadsSequenceToSixDigits() {
            when(nativeQuery.getSingleResult()).thenReturn(1L);

            String result = service.generate();

            assertThat(result).isEqualTo("CUS-000001");
        }

        @Test
        @DisplayName("Handles large sequence numbers beyond 6 digits")
        void generate_handlesLargeSequenceNumber() {
            when(nativeQuery.getSingleResult()).thenReturn(1000000L);

            String result = service.generate();

            assertThat(result).isEqualTo("CUS-1000000");
        }

        @Test
        @DisplayName("Invokes the correct native SQL sequence query")
        void generate_callsNativeQuery() {
            when(nativeQuery.getSingleResult()).thenReturn(1L);

            service.generate();

            verify(entityManager).createNativeQuery("SELECT nextval('customer_number_seq')");
            verify(nativeQuery).getSingleResult();
        }

        @Test
        @DisplayName("Generated number always starts with 'CUS-'")
        void generate_alwaysStartsWithCUSPrefix() {
            when(nativeQuery.getSingleResult()).thenReturn(999L);

            String result = service.generate();

            assertThat(result).startsWith("CUS-");
        }
    }
}
