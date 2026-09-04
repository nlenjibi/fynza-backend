package ecommerce.modules.report.repository;

import ecommerce.common.enums.ReportType;
import ecommerce.modules.report.entity.ReportSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, UUID> {

    List<ReportSchedule> findByCreatedBy(UUID createdBy);

    Page<ReportSchedule> findByStatus(ReportSchedule.ScheduleStatus status, Pageable pageable);

    @Query("SELECT rs FROM ReportSchedule rs WHERE rs.status = 'ACTIVE' AND rs.nextRunAt <= :now")
    List<ReportSchedule> findSchedulesDueForExecution(@Param("now") LocalDateTime now);

    @Query("SELECT rs FROM ReportSchedule rs WHERE rs.reportType = :reportType AND rs.createdBy = :createdBy AND rs.status = 'ACTIVE'")
    List<ReportSchedule> findActiveSchedulesByTypeAndCreator(
            @Param("reportType") ReportType reportType,
            @Param("createdBy") UUID createdBy);

    Optional<ReportSchedule> findByScheduleNameAndCreatedBy(String scheduleName, UUID createdBy);
}
