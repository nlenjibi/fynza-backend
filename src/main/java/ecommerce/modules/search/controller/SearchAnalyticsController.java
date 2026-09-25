package ecommerce.modules.search.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Analytics reads are served via GraphQL (searchAnalyticsSummary query).
// This controller is reserved for future admin mutation endpoints.
@RestController
@RequestMapping("/v1/search/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Search Analytics", description = "Search analytics mutations — Admin only")
public class SearchAnalyticsController {
}
