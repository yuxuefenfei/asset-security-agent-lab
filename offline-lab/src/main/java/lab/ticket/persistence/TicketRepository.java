package lab.ticket.persistence;

import static lab.ticket.persistence.table.TicketTableDef.TICKET;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import java.util.List;
import lab.config.SecurityProperties;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertDraft(TicketPO ticket) {
        mapper.insert(ticket);
    }

    public boolean update(TicketPO ticket) {
        return mapper.update(ticket, false) == 1;
    }

    public List<TicketPO> list(int limit) {
        return mapper.selectListByQuery(query().limit(limit));
    }

    public Page<TicketPO> page(long page, long size) {
        return mapper.paginate(page, size, query());
    }

    private QueryWrapper query() {
        return QueryWrapper.create()
                .where(TICKET.TENANT_ID.eq(security.tenantId()))
                .orderBy(TICKET.CREATED_AT.desc(), TICKET.ID.asc());
    }
}
