package ecommerce.modules.search.service;

import ecommerce.modules.search.repository.SearchSynonymRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SynonymService {

    private final SearchSynonymRepository synonymRepository;

    @Transactional(readOnly = true)
    public String expandQuery(String query) {
        if (query == null || query.isBlank()) return query;
        String[] terms = query.trim().split("\\s+");
        List<String> expanded = new ArrayList<>(Arrays.asList(terms));
        for (String term : terms) {
            synonymRepository.findByTermIgnoreCase(term).ifPresent(syn ->
                expanded.addAll(syn.getSynonymList())
            );
        }
        return String.join(" ", expanded);
    }

    @Transactional(readOnly = true)
    public List<String> findSynonyms(String term) {
        return synonymRepository.findByTermIgnoreCase(term)
                .map(s -> s.getSynonymList())
                .orElse(List.of());
    }
}
