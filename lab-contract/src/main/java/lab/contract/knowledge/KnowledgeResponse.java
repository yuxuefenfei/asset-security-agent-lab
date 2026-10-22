package lab.contract.knowledge;

/** 共享教学知识；source 当前为 training:// 来源标识，并非可访问的官方公告链接。 */
public record KnowledgeResponse(
        String id, String title, String keywords, String body, String source) {}
