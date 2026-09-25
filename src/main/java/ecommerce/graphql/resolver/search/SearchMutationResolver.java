package ecommerce.graphql.resolver.search;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.search.service.SearchHistoryService;
import ecommerce.modules.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchMutationResolver {

    private final SearchHistoryService historyService;
    private final SearchIndexService   indexService;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean clearSearchHistory(@AuthenticationPrincipal UserPrincipal principal) {
        historyService.clearHistory(principal.getId());
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean rebuildSearchIndex() {
        indexService.triggerReindex();
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean retrySearchIndexFailures() {
        indexService.retryFailed();
        return true;
    }
}
