package lab.contract.knowledge;

/**
 * 共享教学知识视图，也作为报告保存知识快照时的数据结构。
 *
 * @param id 知识条目标识，用于去重和详情查询
 * @param title 知识标题
 * @param keywords 检索关键词文本
 * @param body 可核对的知识正文
 * @param source 当前为 training:// 来源标识，并非官方公告链接
 */
public record KnowledgeResponse(
        String id, String title, String keywords, String body, String source) {}
