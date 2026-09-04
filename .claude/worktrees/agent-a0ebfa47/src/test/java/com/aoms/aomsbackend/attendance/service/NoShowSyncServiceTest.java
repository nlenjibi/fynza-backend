package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.repository.NoShowReadModelRepository;
import com.aoms.aomsbackend.attendance.service.impl.NoShowSyncServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoShowSyncServiceTest {

    @Mock
    private NoShowReadModelRepository repository;

    private NoShowSyncServiceImpl service;

    private static final LocalDate SYNC_DATE = LocalDate.of(2026, 3, 15);

    @BeforeEach
    void setUp() {
        service = new NoShowSyncServiceImpl(repository);
    }

    @Test
    void syncForDate_delegatesToRepositoryWithCorrectDate() {
        when(repository.syncFromNoShowRecord(SYNC_DATE)).thenReturn(3);

        service.syncForDate(SYNC_DATE);

        verify(repository).syncFromNoShowRecord(SYNC_DATE);
    }

    @Test
    void syncForDate_returnsInsertedCountFromRepository() {
        when(repository.syncFromNoShowRecord(SYNC_DATE)).thenReturn(5);

        int result = service.syncForDate(SYNC_DATE);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void syncForDate_withNoRecords_returnsZero() {
        when(repository.syncFromNoShowRecord(SYNC_DATE)).thenReturn(0);

        int result = service.syncForDate(SYNC_DATE);

        assertThat(result).isZero();
    }
}
