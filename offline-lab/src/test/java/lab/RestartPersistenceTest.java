package lab;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import lab.contract.analysis.*;
import lab.contract.ticket.*;
import lab.dao.AnalysisRepository;
import lab.dao.AssetMapper;
import lab.service.AnalysisService;
import lab.service.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class RestartPersistenceTest {
    @TempDir Path directory;

    private ConfigurableApplicationContext open(String migrationTarget) {
        return new SpringApplicationBuilder(LabServer.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=jdbc:h2:file:"
                                + directory.resolve("database").toString().replace('\\', '/'),
                        "--spring.flyway.target=" + migrationTarget,
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.main.banner-mode=off",
                        "--lab.security.api-token=restart-token",
                        "--lab.security.approval-code=restart-code");
    }

    @Test
    void upgradingV2AndReopeningRestoresApprovedTicketAndDoesNotReseed() {
        AnalysisReport report;
        TicketResponse approved;
        try (var first = open("2")) {
            assertEquals(3, first.getBean(AssetMapper.class).selectAll().size());
            report =
                    first.getBean(AnalysisService.class)
                            .analyze(new AnalysisRequest("192.0.2.10", "分析", AnalysisMode.DEMO));
            var tickets = first.getBean(TicketService.class);
            var draft = tickets.draft(report.id());
            var edited =
                    tickets.edit(
                            draft.id(),
                            new EditTicketRequest("持久化标题", "保存整改建议", "审核备注", draft.version()));
            approved =
                    tickets.approve(
                            new ApproveRequest(draft.id(), "restart-code", edited.version()));
            var mapper = first.getBean(AssetMapper.class);
            var asset = mapper.selectOneById("ASSET-001");
            asset.setHostname("persisted-host");
            mapper.update(asset);
        }
        try (var second = open("latest")) {
            assertEquals(report, second.getBean(AnalysisRepository.class).get(report.id()));
            assertEquals(approved, second.getBean(TicketService.class).get(approved.id()));
            assertEquals(
                    "persisted-host",
                    second.getBean(AssetMapper.class).selectOneById("ASSET-001").getHostname());
            assertEquals(38, second.getBean(AssetMapper.class).selectAll().size());
            assertEquals(
                    TicketStatus.APPROVED,
                    second.getBean(TicketService.class).draft(report.id()).status());
        }
        try (var third = open("latest")) {
            assertEquals(38, third.getBean(AssetMapper.class).selectAll().size());
            assertEquals(report, third.getBean(AnalysisRepository.class).get(report.id()));
            assertEquals(approved, third.getBean(TicketService.class).get(approved.id()));
        }
    }
}
