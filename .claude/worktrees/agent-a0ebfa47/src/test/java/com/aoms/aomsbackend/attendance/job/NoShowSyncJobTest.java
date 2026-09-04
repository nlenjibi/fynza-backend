package com.aoms.aomsbackend.attendance.job;

import com.aoms.aomsbackend.attendance.service.NoShowSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoShowSyncJobTest {

    @Mock
    private NoShowSyncService noShowSyncService;

    private NoShowSyncJob job;

    @BeforeEach
    void setUp() {
        job = new NoShowSyncJob(noShowSyncService);
    }

    @Test
    void run_delegatesToServiceWithYesterday() {
        LocalDate expectedDate = LocalDate.now().minusDays(1);
        when(noShowSyncService.syncForDate(expectedDate)).thenReturn(2);

        job.run();

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(noShowSyncService).syncForDate(captor.capture());
        assertThat(captor.getValue()).isEqualTo(expectedDate);
    }

    @Test
    void run_whenServiceThrows_doesNotPropagateException() {
        doThrow(new RuntimeException("DB unavailable")).when(noShowSyncService).syncForDate(any());

        assertThatCode(() -> job.run()).doesNotThrowAnyException();
    }
}
