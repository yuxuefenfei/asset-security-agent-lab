package lab.service;

import static com.mybatisflex.core.query.QueryMethods.*;
import static lab.entity.table.KnowledgeTableDef.KNOWLEDGE;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import java.util.List;
import java.util.Locale;
import lab.config.PolicyProperties;
import lab.contract.knowledge.KnowledgeResponse;
import lab.dao.KnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeService {
    private final KnowledgeMapper mapper;
    private final PolicyProperties policy;

    /** 保留原有关键词权重，相关性排序及 LIMIT 全部在数据库执行。 */
    public List<KnowledgeResponse> search(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        if (query.length() > policy.maxQueryLength())
            throw new IllegalArgumentException("检索内容不能超过" + policy.maxQueryLength() + "字");
        QueryColumn rank = null;
        for (String term : query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_.-]+")) {
            if (term.length() < 2) continue;
            String pattern = term.replace("_", "\\_");
            var matches =
                    lower(KNOWLEDGE.TITLE)
                            .like(pattern)
                            .or(lower(KNOWLEDGE.KEYWORDS).like(pattern))
                            .or(lower(KNOWLEDGE.BODY).like(pattern));
            QueryColumn score =
                    case_().when(matches).then(term.startsWith("lab-") ? 5 : 1).else_(0).end();
            rank = rank == null ? score : rank.add(score);
        }
        if (rank == null) return List.of();
        return mapper
                .selectListByQuery(
                        QueryWrapper.create()
                                .select(KNOWLEDGE.ALL_COLUMNS)
                                .where(rank.gt(0))
                                .orderBy(rank.desc(), KNOWLEDGE.ID.asc())
                                .limit(limit))
                .stream()
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
