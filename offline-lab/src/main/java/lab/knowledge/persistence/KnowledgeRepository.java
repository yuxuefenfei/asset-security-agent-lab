package lab.knowledge.persistence;

import static com.mybatisflex.core.query.QueryMethods.*;
import static lab.knowledge.persistence.table.KnowledgeTableDef.KNOWLEDGE;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KnowledgeRepository {
    private final KnowledgeMapper mapper;

    /** 保留原有关键词权重，相关性排序及 LIMIT 全部在数据库执行。 */
    public List<KnowledgePO> search(String query, int limit) {
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
        return mapper.selectListByQuery(
                QueryWrapper.create()
                        .select(KNOWLEDGE.ALL_COLUMNS)
                        .where(rank.gt(0))
                        .orderBy(rank.desc(), KNOWLEDGE.ID.asc())
                        .limit(limit));
    }
}
