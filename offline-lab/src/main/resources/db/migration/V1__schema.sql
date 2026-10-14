CREATE TABLE lab_asset (
    id VARCHAR(64) PRIMARY KEY, ip VARCHAR(45) NOT NULL UNIQUE, tenant_id VARCHAR(64) NOT NULL,
    hostname VARCHAR(200) NOT NULL, os VARCHAR(200) NOT NULL, business VARCHAR(300) NOT NULL,
    criticality VARCHAR(16) NOT NULL CHECK (criticality IN ('LOW','MEDIUM','HIGH')),
    internet_exposed BOOLEAN NOT NULL, owner VARCHAR(100) NOT NULL
);
CREATE INDEX idx_asset_tenant ON lab_asset(tenant_id, id);
CREATE TABLE lab_asset_port (
    id VARCHAR(64) PRIMARY KEY, asset_id VARCHAR(64) NOT NULL REFERENCES lab_asset(id),
    port INT NOT NULL CHECK (port BETWEEN 1 AND 65535), UNIQUE(asset_id, port)
);
CREATE TABLE lab_vulnerability (
    id VARCHAR(64) PRIMARY KEY, asset_id VARCHAR(64) NOT NULL REFERENCES lab_asset(id),
    advisory VARCHAR(100) NOT NULL, name VARCHAR(300) NOT NULL,
    severity VARCHAR(16) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    cvss DOUBLE PRECISION NOT NULL CHECK (cvss BETWEEN 0 AND 10), component VARCHAR(200) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('OPEN','VERIFIED','RESOLVED')),
    scanner VARCHAR(100) NOT NULL, last_seen DATE NOT NULL, verified BOOLEAN NOT NULL
);
CREATE INDEX idx_vulnerability_asset ON lab_vulnerability(asset_id, id);
CREATE TABLE lab_knowledge (
    id VARCHAR(64) PRIMARY KEY, title VARCHAR(400) NOT NULL, keywords VARCHAR(1000) NOT NULL,
    body VARCHAR(10000) NOT NULL, source VARCHAR(500) NOT NULL
);
CREATE TABLE lab_analysis (
    id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL, asset_ip VARCHAR(45) NOT NULL,
    created_at TIMESTAMP(9) WITH TIME ZONE NOT NULL, mode VARCHAR(16) NOT NULL CHECK (mode IN ('DEMO','LLM')),
    risk_score INT NOT NULL CHECK (risk_score BETWEEN 0 AND 100),
    risk_level VARCHAR(16) NOT NULL CHECK (risk_level IN ('LOW','MEDIUM','HIGH')),
    summary CLOB NOT NULL, warning VARCHAR(2000) NOT NULL, status VARCHAR(16) NOT NULL CHECK (status = 'ANALYZED'),
    asset_snapshot CLOB NOT NULL, vulnerabilities_snapshot CLOB NOT NULL
);
CREATE INDEX idx_analysis_tenant_time ON lab_analysis(tenant_id, created_at DESC, id);
CREATE TABLE lab_analysis_evidence (
    id VARCHAR(64) PRIMARY KEY, analysis_id VARCHAR(64) NOT NULL REFERENCES lab_analysis(id),
    position INT NOT NULL, knowledge_id VARCHAR(64) NOT NULL, title VARCHAR(400) NOT NULL,
    keywords VARCHAR(1000) NOT NULL, body VARCHAR(10000) NOT NULL, source VARCHAR(500) NOT NULL,
    UNIQUE(analysis_id, position), UNIQUE(analysis_id, knowledge_id)
);
CREATE TABLE lab_tool_trace (
    id VARCHAR(64) PRIMARY KEY, analysis_id VARCHAR(64) NOT NULL REFERENCES lab_analysis(id),
    step INT NOT NULL, origin VARCHAR(32) NOT NULL CHECK (origin IN ('JAVA_ORCHESTRATOR','LLM_REQUEST')),
    tool VARCHAR(32) NOT NULL CHECK (tool IN ('GET_ASSET','GET_VULNERABILITIES','SEARCH_KNOWLEDGE')),
    arguments_json CLOB NOT NULL, result_count INT NOT NULL, UNIQUE(analysis_id, step)
);
CREATE TABLE lab_ticket (
    id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL,
    analysis_id VARCHAR(64) NOT NULL UNIQUE REFERENCES lab_analysis(id), asset_ip VARCHAR(45) NOT NULL,
    title VARCHAR(200) NOT NULL, status VARCHAR(16) NOT NULL CHECK (status IN ('DRAFT','APPROVED')),
    risk_level VARCHAR(16) NOT NULL CHECK (risk_level IN ('LOW','MEDIUM','HIGH')),
    created_at TIMESTAMP(9) WITH TIME ZONE NOT NULL, approved_by VARCHAR(100), approved_at TIMESTAMP(9) WITH TIME ZONE,
    note VARCHAR(2000), remediation VARCHAR(4000) NOT NULL, version BIGINT DEFAULT 0 NOT NULL CHECK(version >= 0),
    CHECK ((status = 'DRAFT' AND approved_at IS NULL AND approved_by IS NULL)
        OR (status = 'APPROVED' AND approved_at IS NOT NULL AND approved_by IS NOT NULL))
);
CREATE INDEX idx_ticket_tenant_time ON lab_ticket(tenant_id, created_at DESC, id);
