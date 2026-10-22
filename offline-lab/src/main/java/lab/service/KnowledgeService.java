package lab.service;

import static com.mybatisflex.core.query.QueryMethods.*;
import static lab.entity.table.KnowledgeTableDef.KNOWLEDGE;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import java.util.List;
import java.util.Locale;
import lab.config.PolicyProperties;
import lab.contract.api.PageResponse;
import lab.contract.knowledge.KnowledgeResponse;
import lab.dao.KnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeService {
    private final KnowledgeMapper mapper;
    private final PolicyProperties policy;
    private final PagePolicy pages;

    /** 工作台目录按字面子串分页；与下方提供给工具的加权检索保持不同语义。 */
    public PageResponse<KnowledgeResponse> page(long page, long size, String q) {
        pages.validate(page, size);
        String keyword = pages.keyword(q);
        var query = QueryWrapper.create();
        if (!keyword.isEmpty())
            query.where(
                    KNOWLEDGE
                            .TITLE
                            .like(keyword)
                            .or(KNOWLEDGE.KEYWORDS.like(keyword))
                            .or(KNOWLEDGE.BODY.like(keyword)));
        var result = mapper.paginate(page, size, query.orderBy(KNOWLEDGE.ID.asc()));
        return new PageResponse<>(
                result.getRecords().stream()
                        .map(
                                k ->
                                        new KnowledgeResponse(
                                                k.getId(),
                                                k.getTitle(),
                                                k.getKeywords(),
                                                k.getBody(),
                                                k.getSource()))
                        .toList(),
                page,
                size,
                result.getTotalRow());
    }

    public KnowledgeResponse get(String id) {
        var k = mapper.selectOneById(id);
        if (k == null) throw new IllegalArgumentException("知识条目不存在");
        return new KnowledgeResponse(
                k.getId(), k.getTitle(), k.getKeywords(), k.getBody(), k.getSource());
    }

    /** 保留原有关键词权重，相关性排序及 LIMIT 全部在数据库执行。 */
    public List<KnowledgeResponse> search(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        if (query.length() > policy.maxQueryLength())
            throw new IllegalArgumentException("检索内容不能超过" + policy.maxQueryLength() + "字");
        // 知识是共享训练资料；此处没有文档级租户授权，不能直接替代生产检索边界。
        QueryColumn rank = null;
        for (String term : query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_.-]+")) {
            if (term.length() < 2) continue;
            String pattern = term.replace("_", "\\_");
            var matches =
                    lower(KNOWLEDGE.TITLE)
                            .like(pattern)
                            .or(lower(KNOWLEDGE.KEYWORDS).like(pattern))
                            .or(lower(KNOWLEDGE.BODY).like(pattern));
            // 编号词项提高权重但仍是子串匹配，不等于结构化 advisory 严格相等查询。
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
