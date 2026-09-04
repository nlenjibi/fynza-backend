package ecommerce.modules.report.repository;

import ecommerce.common.enums.ReportFormat;
import ecommerce.common.enums.ReportType;
import ecommerce.modules.report.entity.Report;
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
public interface ReportRepository extends JpaRepository<Report, UUID> {

    Optional<Report> findByReportNumber(String reportNumber);

    Page<Report> findByCreatedBy(UUID createdBy, Pageable pageable);

    Page<Report> findByReportType(ReportType reportType, Pageable pageable);

    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);

    @Query("SELECT r FROM Report r WHERE " +
            "(:reportType IS NULL OR r.reportType = :reportType) AND " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:createdBy IS NULL OR r.createdBy = :createdBy) AND " +
            "(:dateFrom IS NULL OR r.createdAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR r.createdAt <= :dateTo)")
    Page<Report> searchReports(
            @Param("reportType") ReportType reportType,
            @Param("status") Report.ReportStatus status,
            @Param("createdBy") UUID createdBy,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    List<Report> findByStatusIn(List<Report.ReportStatus> statuses);
}
