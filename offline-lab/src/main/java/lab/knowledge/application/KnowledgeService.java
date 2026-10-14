package lab.knowledge.application;

import java.util.List;
import lab.config.PolicyProperties;
import lab.contract.knowledge.KnowledgeResponse;
import lab.knowledge.persistence.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeService {
    private final KnowledgeRepository repository;
    private final PolicyProperties policy;

    public List<KnowledgeResponse> search(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        if (query.length() > policy.maxQueryLength())
            throw new IllegalArgumentException("检索内容不能超过" + policy.maxQueryLength() + "字");
        return repository.search(query, limit).stream()
                .map(
                        k ->
                                new KnowledgeResponse(
                                        k.getId(),
                                        k.getTitle(),
                                        k.getKeywords(),
                                        k.getBody(),
                                        k.getSource()))
                .toList();
    }
}
