package ecommerce.modules.refund.service.impl;

import ecommerce.modules.refund.dto.ReturnAnalyticsResponse;
import ecommerce.modules.refund.enums.ReturnStatus;
import ecommerce.modules.refund.repository.ReturnRepository;
import ecommerce.modules.refund.service.ReturnAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnAnalyticsServiceImpl implements ReturnAnalyticsService {

    private final ReturnRepository returnRepository;

    @Override
    public ReturnAnalyticsResponse getAnalytics() {
        long total    = returnRepository.count();
        long resolved = returnRepository.countByStatus(ReturnStatus.RESOLVED);
        long failed   = returnRepository.countByStatus(ReturnStatus.FAILED);
        long pending  = total - resolved - failed
                - returnRepository.countByStatus(ReturnStatus.CANCELLED)
                - returnRepository.countByStatus(ReturnStatus.REJECTED)
                - returnRepository.countByStatus(ReturnStatus.EXPIRED);

        List<Map<String, Object>> statusRows  = returnRepository.countGroupByStatus();
        List<Map<String, Object>> reasonRows  = returnRepository.countGroupByReason();

        List<ReturnAnalyticsResponse.ReturnStatusCount> byStatus = statusRows.stream()
                .map(r -> ReturnAnalyticsResponse.ReturnStatusCount.builder()
                        .status(r.get("status").toString())
                        .count(((Number) r.get("count")).longValue())
                        .build())
                .collect(Collectors.toList());

        List<ReturnAnalyticsResponse.ReturnReasonCount> byReason = reasonRows.stream()
                .map(r -> ReturnAnalyticsResponse.ReturnReasonCount.builder()
                        .reason(r.get("reason").toString())
                        .count(((Number) r.get("count")).longValue())
                        .build())
                .collect(Collectors.toList());

        return ReturnAnalyticsResponse.builder()
                .totalReturns(total)
                .resolvedReturns(resolved)
                .pendingReturns(Math.max(0, pending))
                .failedReturns(failed)
                .countByStatus(byStatus)
                .countByReason(byReason)
                .build();
    }
}
