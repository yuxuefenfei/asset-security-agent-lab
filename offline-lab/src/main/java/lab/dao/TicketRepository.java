package lab.dao;

import static lab.entity.table.TicketTableDef.TICKET;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import java.util.List;
import lab.config.SecurityProperties;
import lab.contract.analysis.RiskLevel;
import lab.contract.ticket.TicketStatus;
import lab.entity.TicketPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class TicketRepository {
    private final TicketMapper mapper;
    private final SecurityProperties security;

    public TicketPO find(String id) {
        return mapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(TICKET.ID.eq(id))
                        .and(TICKET.TENANT_ID.eq(security.tenantId())));
    }

    public TicketPO findForAnalysis(String analysisId) {
        return mapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(TICKET.ANALYSIS_ID.eq(analysisId))
                        .and(TICKET.TENANT_ID.eq(security.tenantId())));
    }

    // 唯一键竞争失败只回滚这次插入，外层才能继续读取已创建的草稿。
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertDraft(TicketPO ticket) {
        mapper.insert(ticket);
    }

    /** TicketPO 的 version 注解让 Flex 加入版本条件并自增；零行表示并发更新冲突。 */
    public boolean update(TicketPO ticket) {
        return mapper.update(ticket, false) == 1;
    }

    public List<TicketPO> list(int limit) {
        return mapper.selectListByQuery(query().limit(limit));
    }

    public Page<TicketPO> page(long page, long size) {
        return mapper.paginate(page, size, query());
    }

    public Page<TicketPO> page(
            long page,
            long size,
            String q,
            TicketStatus status,
            RiskLevel riskLevel,
            String analysisId) {
        var query = query();
        if (!q.isEmpty())
            query.and(TICKET.TITLE.like(q).or(TICKET.ASSET_IP.like(q)).or(TICKET.ID.like(q)));
        if (status != null) query.and(TICKET.STATUS.eq(status));
        if (riskLevel != null) query.and(TICKET.RISK_LEVEL.eq(riskLevel));
        if (analysisId != null && !analysisId.isBlank())
            query.and(TICKET.ANALYSIS_ID.eq(analysisId));
        return mapper.paginate(page, size, query);
    }

    // 所有工单列表先限定服务端租户，再用时间与 ID 保持跨页顺序稳定。
    private QueryWrapper query() {
        return QueryWrapper.create()
                .where(TICKET.TENANT_ID.eq(security.tenantId()))
                .orderBy(TICKET.CREATED_AT.desc(), TICKET.ID.asc());
    }
}
