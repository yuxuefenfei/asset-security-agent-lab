package lab.service;

import static lab.entity.table.AnalysisTableDef.ANALYSIS;
import static lab.entity.table.AssetTableDef.ASSET;
import static lab.entity.table.TicketTableDef.TICKET;

import com.mybatisflex.core.query.QueryWrapper;
import lab.config.SecurityProperties;
import lab.contract.analysis.RiskLevel;
import lab.contract.dashboard.DashboardResponse;
import lab.contract.ticket.TicketStatus;
import lab.dao.AnalysisMapper;
import lab.dao.AssetMapper;
import lab.dao.KnowledgeMapper;
import lab.dao.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 工作台计数服务，在数据库执行聚合；统计口径分别对应资产、报告和工单。 */
@Service
@RequiredArgsConstructor
public class DashboardService {
    private final AssetMapper assets;
    private final AnalysisMapper analyses;
    private final TicketMapper tickets;
    private final KnowledgeMapper knowledge;
    private final SecurityProperties security;

    // 高风险按报告计数，待办按 DRAFT 工单计数；知识是共享目录，不是按资产风险聚合。
    /** 在只读事务中统计工作台指标；高风险按报告计数，待审核按草稿工单计数。 */
    @Transactional(readOnly = true)
    public DashboardResponse get() {
        return new DashboardResponse(
                assets.selectCountByQuery(
                        QueryWrapper.create().where(ASSET.TENANT_ID.eq(security.tenantId()))),
                assets.selectCountByQuery(
                        QueryWrapper.create()
                                .where(ASSET.TENANT_ID.eq(security.tenantId()))
                                .and(ASSET.INTERNET_EXPOSED.eq(true))),
                analyses.selectCountByQuery(
                        QueryWrapper.create().where(ANALYSIS.TENANT_ID.eq(security.tenantId()))),
                analyses.selectCountByQuery(
                        QueryWrapper.create()
                                .where(ANALYSIS.TENANT_ID.eq(security.tenantId()))
                                .and(ANALYSIS.RISK_LEVEL.eq(RiskLevel.HIGH))),
                tickets.selectCountByQuery(
                        QueryWrapper.create()
                                .where(TICKET.TENANT_ID.eq(security.tenantId()))
                                .and(TICKET.STATUS.eq(TicketStatus.DRAFT))),
                tickets.selectCountByQuery(
                        QueryWrapper.create()
                                .where(TICKET.TENANT_ID.eq(security.tenantId()))
                                .and(TICKET.STATUS.eq(TicketStatus.APPROVED))),
                knowledge.selectCountByQuery(QueryWrapper.create()));
    }
}
