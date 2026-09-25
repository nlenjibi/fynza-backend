package ecommerce.modules.search.scheduler;

import ecommerce.modules.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchIndexRetryJob {

    private final SearchIndexService searchIndexService;

    @Scheduled(fixedDelay = 300_000)
    public void retryFailedIndexDocuments() {
        searchIndexService.retryFailed();
    }
}
