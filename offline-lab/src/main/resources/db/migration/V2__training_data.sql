-- Flyway 每个版本只执行一次，重启不会重置已修改的业务记录。
INSERT INTO lab_asset VALUES
('ASSET-001','192.0.2.10','tenant-blue','web-prod-01','Ubuntu Linux','订单服务（模拟）','HIGH',TRUE,'security-team'),
('ASSET-002','192.0.2.20','tenant-blue','db-test-02','Linux','测试数据库（模拟）','LOW',FALSE,'test-team'),
('ASSET-OTHER','198.51.100.9','tenant-red','secret-tenant-host','Linux','其他租户','HIGH',TRUE,'another-team');
INSERT INTO lab_asset_port VALUES
('PORT-1','ASSET-001',22),('PORT-2','ASSET-001',80),('PORT-3','ASSET-001',443),('PORT-4','ASSET-001',8080),
('PORT-5','ASSET-002',22),('PORT-6','ASSET-002',3306),('PORT-7','ASSET-OTHER',443);
INSERT INTO lab_vulnerability VALUES
('VULN-101','ASSET-001','LAB-2026-0001','演示用 Web 框架版本缺陷','HIGH',8.6,'DemoWeb 1.2','OPEN','demo-scanner','2026-10-01',FALSE),
('VULN-102','ASSET-001','LAB-2026-0002','演示用弱配置风险','MEDIUM',5.3,'DemoConfig','OPEN','demo-scanner','2026-09-29',FALSE),
('VULN-103','ASSET-002','LAB-2026-0003','演示用数据库补丁缺失','MEDIUM',5.9,'DemoDB','OPEN','demo-scanner','2026-10-03',FALSE),
('OTHER-1','ASSET-OTHER','PRIVATE','其他租户记录','CRITICAL',9.8,'Private','OPEN','private-scanner','2026-10-01',FALSE);
INSERT INTO lab_knowledge VALUES
('KB-001','LAB-2026-0001 DemoWeb 修复公告（虚构）','LAB-2026-0001 DemoWeb Web 框架 补丁 版本','核实实际运行的 DemoWeb 版本；测试兼容后升级到内部维护的已修复版本；整改后重新扫描。不得仅根据端口开放判定漏洞已被利用。','training://kb/KB-001'),
('KB-002','LAB-2026-0002 Web 基线配置（虚构）','LAB-2026-0002 DemoConfig 配置 基线','核对配置基线、限制管理接口访问、复核暴露路径，整改后进行配置审计。','training://kb/KB-002'),
('KB-003','LAB-2026-0003 数据库补丁规范（虚构）','LAB-2026-0003 DemoDB 数据库 修复','确认维护窗口和数据库备份，验证测试环境兼容性后应用补丁，完成回归测试与漏洞复测。','training://kb/KB-003'),
('KB-004','安全工单处理规范','安全 工单 审批 风险 处置 修复','AI 仅辅助生成分析说明；用户通过独立接口创建整改工单草稿并审批。教学平台仅记录模拟审批，不创建外部正式工单；真实整改后必须由扫描器或管理员重新验证。','training://policy/SEC-001');
