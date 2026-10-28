package lab.contract.analysis;

/** 报告保存后的业务状态；当前只保存成功完成的分析，不持久化运行中或失败任务。 */
public enum AnalysisStatus {
    /** 分析已成功完成并形成报告；当前没有持久化中的排队或运行状态。 */
    ANALYZED
}
