package lab.contract.knowledge;

public record KnowledgeResponse(
        String id, String title, String keywords, String body, String source) {}
