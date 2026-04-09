package kr.ac.ajou.ie.processchatbot.dto;

public record ChatResponse(
	String answer,
	boolean usedDatabase,
	String queryType,
	String selectedAction,
	int resultCount,
	String referenceSummary
) {
}
